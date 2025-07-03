package ai.functionals.api.neura.service;

import ai.functionals.api.neura.jpa.entity.SoftwareDesigner;
import ai.functionals.api.neura.jpa.repo.SoftwareDesignerRepo;
import ai.functionals.api.neura.jpa.repo.SoftwareDesignerVersionRepo;
import ai.functionals.api.neura.model.commons.AppException;
import ai.functionals.api.neura.model.commons.AppUser;
import ai.functionals.api.neura.model.enums.DocSessionStatus;
import ai.functionals.api.neura.model.req.LLMRequest;
import ai.functionals.api.neura.model.rsp.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import functionals.designer.v1.DesignerServiceGrpc.DesignerServiceFutureStub;
import functionals.designer.v1.SoftwareDesigner.GetDesignerRequest;
import functionals.designer.v1.SoftwareDesigner.GetDesignerResponse;
import functionals.designer.v1.SoftwareDesigner.GetPreDesignerRequest;
import functionals.designer.v1.SoftwareDesigner.GetPreDesignerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Objects;

import static ai.functionals.api.neura.model.enums.SoftwareDesignerStatus.IN_PROGRESS;
import static ai.functionals.api.neura.util.AppUtil.*;
import static ai.functionals.api.neura.util.ProtoUtils.protobufPrint;
import static functionals.designer.v1.SoftwareDesigner.ResponseType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwareDesignerService {
    @GrpcClient("designer-service")
    private DesignerServiceFutureStub designerLLM;
    private final RedisTemplate<String, LLMResponse> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate2;
    private final SoftwareDesignerRepo softwareDesignerRepo;
    private final SoftwareDesignerVersionRepo softwareDesignerVersionRepo;
    private final ObjectMapper llmObjectMapper;
    private final Map<String, SseEmitter> emitters;
    private final SseEmitterService sseEmitterService;
    private final StorageService storageService;
    private final UserService userService;

    @Value("${app.schema.version.designer}")
    private Integer designerSchemaVersion;
    @Value("${app.schema.version.clarification}")
    private String clarificationSchemaVersion;
    @Value("${app.s3.bucket.documents}")
    private String docsBucket;

    private final ObjectMapper mapper = new ObjectMapper();

    // Tobe removed and should come from Proto
    private final Map<String, String> keysToParse = Map.of(
            DOCUMENT.name(), "design_ready_prd",
            QUESTIONS.name(), "clarification_questions",
            FEEDBACK.name(), "design_ready_prd"
    );

    /**
     * Begin the design process with an initial idea
     * Trigger LLM to generate clarification questions and wait on Async thread
     *
     * @param prompt
     * @param user
     * @param sessionId
     */
    public void beginWithIdea(String prompt, AppUser user, String sessionId) {
        log.info("Beginning design session: {} with prompt: {}", sessionId, prompt);
        userService.validateUserCredits(user);
        try {
            SoftwareDesigner softwareDesigner = new SoftwareDesigner();
            softwareDesigner.setSessionId(sessionId);
            softwareDesigner.setTenantId(user.getTenantId());
            softwareDesigner.setOwnerId(user.getId());
            softwareDesigner.setStatus(IN_PROGRESS);
            softwareDesigner.setDocumentVersion(0);
            softwareDesigner.setSchemaVersion(designerSchemaVersion);
            softwareDesigner.setDocSessionStatus(DocSessionStatus.STARTED);
            softwareDesignerRepo.save(softwareDesigner);
            GetPreDesignerRequest request = GetPreDesignerRequest.newBuilder()
                    .setProductDescription(prompt)
                    .setClarificationSchema(getClarificationSchemaFromResources(clarificationSchemaVersion))
                    .setDesignerSchema(getSchemaFromResources(String.valueOf(designerSchemaVersion), true))
                    .build();
            log.info("Request to LLM: {}", request);
            ListenableFuture<GetPreDesignerResponse> responseFuture = designerLLM.getPreDesigner(request);
            // read the response by waiting for it in a thread and store it in the designSessions map
            new Thread(() -> {
                try {
                    GetPreDesignerResponse response = responseFuture.get();
                    String rspStr = protobufPrint(response);
                    log.info("Initial response from LLM: {}", rspStr);
                    String responseType = response.getResponseType().name();
                    if (keysToParse.containsKey(responseType)) {
                        String value = parseResponseAndExtractValue(response.getAgentSnapshot(), responseType);
                        redisTemplate.convertAndSend(LLM_RESPONSE_TOPIC, LLMResponse.builder().sessionId(sessionId)
                                .response(value)
                                .docSessionStatus(DocSessionStatus.CLARIFICATION_GENERATED_BY_LLM)
                                .responseType(response.getResponseType())
                                .preDesignerRequest(protobufPrint(request))
                                .creditsSpent((long) (response.getCreditsSpent() * 100L))
                                .documentVersion(1)
                                .schemaVersion(designerSchemaVersion)
                                .build());
                        stringRedisTemplate2.opsForValue().set(SNAPSHOT_PREFIX + sessionId, response.getAgentSnapshot());
                    }
                } catch (Exception e) {
                    log.error("Failed to process design request", e);
                    sseEmitterService.send(sessionId, "error: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process design request", e);
        }
    }

    /**
     * Submit the clarifications input from user
     * 1) Trigger LLM to generate initial high level documentation
     * 2) Trigger LLM to generate detailed refinement
     * 3) wait on Async thread
     *
     * @param prompt
     * @param user
     * @param sessionId
     */
    public void submitClarificationsPrompt(String prompt, AppUser user, String sessionId) {
        log.info("Submitting clarifications response session: {} prompt: {}", sessionId, prompt);
        userService.validateUserCredits(user);
        try {
            String agentSnapshot = stringRedisTemplate2.opsForValue().get(SNAPSHOT_PREFIX + sessionId);
            GetPreDesignerRequest request = GetPreDesignerRequest.newBuilder().setAgentSnapshot(agentSnapshot == null ? "" : agentSnapshot)
                    .setClarificationAnswer(prompt)
                    .setClarificationSchema(getClarificationSchemaFromResources(clarificationSchemaVersion))
                    .setDesignerSchema(getSchemaFromResources(String.valueOf(designerSchemaVersion), true))
                    .build();
            log.info("Initial PRD request: {}", protobufPrint(request));
            softwareDesignerRepo.updateDocSessionStatus(user.getTenantId(), sessionId, DocSessionStatus.CLARIFICATION_SUBMITTED_BY_USER);
            ListenableFuture<GetPreDesignerResponse> responseFuture = designerLLM.getPreDesigner(request);
            new Thread(() -> {
                try {
                    GetPreDesignerResponse response = responseFuture.get();
                    String rspStr = protobufPrint(response);
                    log.info("Clarification response session: {} from LLM: {}", sessionId, rspStr);
                    String responseType = response.getResponseType().name();
                    if (keysToParse.containsKey(responseType)) {
                        String llmRsp = parseResponseAndExtractValue(response.getAgentSnapshot(), responseType);
                        if (StringUtils.isBlank(llmRsp)) {
                            log.info("Empty response1 from LLM for session: {}", sessionId);
                            return;
                        }
                        redisTemplate.convertAndSend(LLM_RESPONSE_TOPIC, LLMResponse.builder().sessionId(sessionId)
                                .responseType(response.getResponseType())
                                .productDesign(llmRsp)
                                .docSessionStatus(DocSessionStatus.INITIAL_DOC_GENERATED_BY_LLM)
                                .creditsSpent((long) (response.getCreditsSpent() * 100L))
                                .documentVersion(1)
                                .schemaVersion(designerSchemaVersion)
                                .build());
                        stringRedisTemplate2.opsForValue().set(SNAPSHOT_PREFIX + sessionId, response.getAgentSnapshot());
                        if (!response.getResponseType().equals(QUESTIONS)) { // Trigger the detailed refinement on llm
                            String agentSnapshot2 = response.getAgentSnapshot();
                            GetDesignerRequest request2 = GetDesignerRequest.newBuilder().setAgentSnapshot(agentSnapshot2).build();
                            log.info("Refinement PRD request: {}", protobufPrint(request2));
                            softwareDesignerRepo.updateDocSessionStatus(user.getTenantId(), sessionId, DocSessionStatus.DETAILED_DOC_REQUESTED_BY_SYSTEM);
                            ListenableFuture<GetDesignerResponse> responseFuture2 = designerLLM.getDesigner(request2);
                            new Thread(() -> {
                                try {
                                    GetDesignerResponse response2 = responseFuture2.get();
                                    String rspStr2 = protobufPrint(response2);
                                    log.info("Refinement PRD response session: {} from LLM: {}", sessionId, rspStr2);
                                    String responseType2 = response.getResponseType().name();
                                    if (keysToParse.containsKey(responseType2)) {
                                        String llmRsp2 = parseResponseAndExtractValue(response2.getAgentSnapshot(), responseType2);
                                        if (StringUtils.isBlank(llmRsp)) {
                                            log.info("Empty response2 from LLM for session: {}", sessionId);
                                            return;
                                        }
                                        redisTemplate.convertAndSend(LLM_RESPONSE_TOPIC, LLMResponse.builder().sessionId(sessionId)
                                                .responseType(response.getResponseType())
                                                .productDesign(llmRsp2)
                                                .docSessionStatus(DocSessionStatus.DETAILED_DOC_GENERATED_BY_LLM)
                                                .creditsSpent((long) (response2.getCreditsSpent() * 100L))
                                                .build());
                                        stringRedisTemplate2.opsForValue().set(SNAPSHOT_PREFIX + sessionId, response2.getAgentSnapshot());
                                    }
                                } catch (Exception e) {
                                    log.error("Failed to process design request", e);
                                    sseEmitterService.send(sessionId, "error: " + e.getMessage());
                                }
                            }).start();
                            sseEmitterService.send(sessionId, "loading_detailed_design");
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process design request", e);
                    sseEmitterService.send(sessionId, "error: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process design request", e);
        }
    }

    /**
     * Submit the feedback input from user to LLM
     * Wait for response from LLM on an Async Thread
     *
     * @param request
     * @param user
     * @param sessionId
     * @param feedbackId
     */
    public void submitFeedbackPrompt(LLMRequest request, AppUser user, String sessionId, String feedbackId) {
        log.info("Submitting feedback session: {} prompt: {}", sessionId, request.getPrompt());
        userService.validateUserCredits(user);
        try {
            SoftwareDesigner designer = softwareDesignerRepo.findByTenantIdAndOwnerIdAndSessionId(user.getTenantId(),
                            user.getId(), sessionId).stream().findFirst()
                    .orElseThrow(() -> new AppException("Software designer not found for session: " + sessionId));
            // String doc = awsS3Service.getDoc(user.getTenantId(), sessionId, designer.getDocumentVersion());
            String agentSnapShot = stringRedisTemplate2.opsForValue().get(SNAPSHOT_PREFIX + sessionId);
            GetDesignerRequest getDesignerRequest = GetDesignerRequest.newBuilder()
                    .setAgentSnapshot(agentSnapShot == null ? "" : agentSnapShot)
                    .setSectionName(request.getSectionName())
                    .setFeedback(request.getPrompt())
                    .build();
            softwareDesignerRepo.updateDocSessionStatus(user.getTenantId(), sessionId, DocSessionStatus.FEEDBACK_REQUESTED_BY_USER);
            log.info("Feedback PRD request: {}", protobufPrint(getDesignerRequest));
            ListenableFuture<GetDesignerResponse> responseFuture = designerLLM.getDesigner(getDesignerRequest);
            new Thread(() -> {
                try {
                    GetDesignerResponse response = responseFuture.get();
                    String rspStr = protobufPrint(response);
                    log.info("Feedback response session: {} from LLM: {}", sessionId, rspStr);
                    String responseType = response.getResponseType().name();
                    if (keysToParse.containsKey(responseType)) {
                        String llmRsp = parseResponseAndExtractValue(response.getAgentSnapshot(), responseType);
                        if (StringUtils.isBlank(llmRsp)) {
                            log.info("Empty response from LLM for session: {}", sessionId);
                            return;
                        }
                        redisTemplate.convertAndSend(LLM_RESPONSE_TOPIC, LLMResponse.builder().sessionId(sessionId)
                                .responseType(FEEDBACK)
                                .preDesignerRequest(request.getPrompt())
                                .productDesign(llmRsp)
                                .creditsSpent((long) (response.getCreditsSpent() * 100L))
                                .llmResponseDescription(response.getLlmResponseDescription())
                                .docSessionStatus(DocSessionStatus.FEEDBACK_GENERATED_BY_LLM)
                                .sectionPath(request.getSectionPath())
                                .build());
                        stringRedisTemplate2.opsForValue().set(SNAPSHOT_PREFIX + sessionId, response.getAgentSnapshot());
                    }
                } catch (Exception e) {
                    log.error("Failed to process design request", e);
                    sseEmitterService.send(sessionId, "error: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process design request", e);
        }
    }

    public SoftwareDesigner findBySessionId(AppUser user, String slug) {
        log.info("Finding software designer by slug: {}", slug);
        return softwareDesignerRepo.findByTenantIdAndOwnerIdAndSessionId(user.getTenantId(), user.getId(), slug)
                .stream().findFirst().orElseThrow(() -> new AppException("Software designer not found for slug " + slug));
    }

    public LLMResponse getCurrentData(AppUser user, String sessionId) {
        SoftwareDesigner bySessionId = softwareDesignerRepo.findByTenantIdAndOwnerIdAndSessionId(
                        user.getTenantId(), user.getId(), sessionId).stream().findFirst()
                .orElseThrow(() -> new AppException("Not found for " + sessionId));
        String doc = storageService.getDoc(docsBucket, user.getTenantId(), sessionId, bySessionId.getDocumentVersion());
        try {
            Object json = mapper.readValue(doc, Object.class); // parse the JSON string
            String cleanedDoc = mapper.writeValueAsString(json);
            return LLMResponse.builder().sessionId(sessionId)
                    .response(cleanedDoc)
                    .documentVersion(bySessionId.getDocumentVersion())
                    .schemaVersion(bySessionId.getSchemaVersion())
                    .build();
        } catch (Exception e) {
            log.error("Error parsing stored document", e);
            throw new RuntimeException("Failed to parse stored document: " + e);
        }
    }

    public Page<SoftwareDesigner> findAll(AppUser currentUser, Pageable pageable) {
        return softwareDesignerRepo.findAllByTenantIdAndOwnerId(currentUser.getTenantId(),
                currentUser.getId(), pageable);
    }

    public SoftwareDesigner update(AppUser currentUser, SoftwareDesigner designer) {
        SoftwareDesigner designerDB = softwareDesignerRepo.findByTenantIdAndOwnerIdAndSessionId(
                        currentUser.getTenantId(), currentUser.getId(), designer.getSessionId()).stream().findFirst()
                .orElseThrow(() -> new AppException("Software designer not found session: " + designer.getSessionId()));
        if (!Objects.equals(designer.getContributors(), designerDB.getContributors())) {
            designerDB.setContributors(designer.getContributors());
        }
        if (!Objects.equals(designer.getStatus(), designerDB.getStatus())) {
            designerDB.setStatus(designer.getStatus());
        }
        return softwareDesignerRepo.save(designer);
    }

    public void deleteById(AppUser currentUser, String sessionId) {
        SoftwareDesigner designer = softwareDesignerRepo.findByTenantIdAndOwnerIdAndSessionId(currentUser.getTenantId(),
                        currentUser.getId(), sessionId).stream().findFirst()
                .orElseThrow(() -> new AppException("Software designer not found for session: " + sessionId));
        storageService.deleteDocs(docsBucket, currentUser.getTenantId(), sessionId);
        softwareDesignerRepo.delete(designer);
    }

    public void rollback(AppUser currentUser, String sessionId, Integer version) {
        SoftwareDesigner designer = softwareDesignerRepo.findByTenantIdAndOwnerIdAndSessionId(currentUser.getTenantId(),
                        currentUser.getId(), sessionId).stream().findFirst()
                .orElseThrow(() -> new AppException("Software designer not found for session: " + sessionId));
        Integer currentDocumentVersion = designer.getDocumentVersion();
        designer.setDocumentVersion(version);
        log.info("Rolling back document {} version from {} to {}", sessionId, currentDocumentVersion, version);
        String doc = storageService.getDoc(docsBucket, currentUser.getTenantId(), sessionId, version);
        redisTemplate.convertAndSend(LLM_RESPONSE_TOPIC, LLMResponse.builder().sessionId(sessionId)
                .productDesign(doc)
                .docSessionStatus(DocSessionStatus.REVERTED_BY_USER)
                .responseType(version <= 2 ? DOCUMENT : FEEDBACK)
                .documentVersion(version)
                .schemaVersion(designerSchemaVersion)
                .build());
        softwareDesignerRepo.save(designer);
        softwareDesignerVersionRepo.deleteByOwnerIdAndSessionIdAndCurrentDocumentVersionGreaterThan(currentUser.getId(), sessionId, version);
        log.info("Deleted all versions greater than {} for sessionId {}", version, sessionId);
    }
}
