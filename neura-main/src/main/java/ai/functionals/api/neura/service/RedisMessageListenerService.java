package ai.functionals.api.neura.service;

import ai.functionals.api.neura.jpa.entity.SoftwareDesigner;
import ai.functionals.api.neura.jpa.entity.SoftwareDesignerVersion;
import ai.functionals.api.neura.jpa.repo.SoftwareDesignerRepo;
import ai.functionals.api.neura.jpa.repo.SoftwareDesignerVersionRepo;
import ai.functionals.api.neura.model.commons.AppException;
import ai.functionals.api.neura.model.rsp.LLMResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static ai.functionals.api.neura.util.AppUtil.LLM_RESPONSE_TOPIC;
import static functionals.designer.v1.SoftwareDesigner.ResponseType.FEEDBACK;
import static functionals.designer.v1.SoftwareDesigner.ResponseType.QUESTIONS;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageListenerService {
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedisTemplate<String, String> stringRedisTemplate2;
    private final ObjectMapper llmObjectMapper;
    private final SoftwareDesignerRepo softwareDesignerRepo;
    private final SoftwareDesignerVersionRepo softwareDesignerVersionRepo;
    private final StorageService storageService;
    private final SseEmitterService sseEmitterService;
    private final UserService userService;

    @Value("${app.schema.version.designer}")
    private Integer designerSchemaVersion;
    @Value("${app.s3.bucket.documents}")
    private String docsBucket;

    @PostConstruct
    private void init() {
        redisMessageListenerContainer.addMessageListener((message, pattern) -> {
            try {
                log.info("Received redis message: {}", new String(message.getBody(), StandardCharsets.UTF_8));
                LLMResponse response = llmObjectMapper.readValue(message.getBody(), LLMResponse.class);
                sseEmitterService.send(response.getSessionId(), response);

                if (response.getProductDesign() != null || response.getResponseType() == QUESTIONS) {
                    SoftwareDesigner designer = softwareDesignerRepo.findBySessionId(response.getSessionId())
                            .orElseThrow(() -> new AppException("SoftwareDesigner not found for " + response.getSessionId()));
                    int currentDocumentVersion = designer.getDocumentVersion() == null ? 1 : designer.getDocumentVersion() + 1;
                    String docContent = StringUtils.isBlank(response.getProductDesign()) && response.getResponseType() == QUESTIONS ?
                            response.getResponse() : response.getProductDesign();
                    storageService.putDoc(docsBucket, designer.getTenantId(), response.getSessionId(), currentDocumentVersion, docContent);
                    log.info("Updating SoftwareDesigner with sessionId: {}", response.getSessionId());
                    designer.setDocumentVersion(currentDocumentVersion);
                    designer.setDocSessionStatus(response.getDocSessionStatus());
                    setTitle(designer, response);
                    softwareDesignerRepo.save(designer);
                    log.info("Updated SoftwareDesigner with sessionId: {} ver:{}", response.getSessionId(), currentDocumentVersion);
                    if (FEEDBACK.equals(response.getResponseType())) {
                        SoftwareDesignerVersion version = makeNewSoftwareDesignerVersion(designer, response, currentDocumentVersion);
                        softwareDesignerVersionRepo.save(version);
                    }
                    // Record credits used
                    userService.useCredits(designer.getOwnerId(), response.getCreditsSpent());
                }
            } catch (Exception e) {
                log.error("Failed to process LLM response", e);
            }
        }, new ChannelTopic(LLM_RESPONSE_TOPIC));
    }

    private void setTitle(SoftwareDesigner designer, LLMResponse response) {
        try {
            String content = StringUtils.isNotBlank(response.getProductDesign()) ? response.getProductDesign()
                    : response.getResponse();
            JsonNode titleNode = llmObjectMapper.readTree(content).get("title");
            String title = Objects.nonNull(titleNode) ? titleNode.asText() : null;
            if (StringUtils.isNotBlank(title)) {
                if (StringUtils.isBlank(designer.getTitle()) || !StringUtils.equalsIgnoreCase(designer.getTitle(), title)) {
                    designer.setTitle(title);
                    log.info("Setting title for SoftwareDesigner with sessionId: {} to {}", designer.getSessionId(), title);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing title from LLM response", e);
        }
    }

    private static SoftwareDesignerVersion makeNewSoftwareDesignerVersion(SoftwareDesigner designer, LLMResponse response, int documentVersion) {
        SoftwareDesignerVersion version = new SoftwareDesignerVersion();
        version.setSessionId(designer.getSessionId());
        version.setOwnerId(designer.getOwnerId());
        version.setSectionPaths(response.getSectionPath());
        version.setPrompt(response.getPreDesignerRequest());
        version.setPreviousDocumentVersion(documentVersion - 1);
        version.setCurrentDocumentVersion(documentVersion);
        version.setSchemaVersion(designer.getSchemaVersion());
        version.setSoftwareDesigner(designer);
        version.setLlmResponseDescription(response.getLlmResponseDescription());
        return version;
    }

}
