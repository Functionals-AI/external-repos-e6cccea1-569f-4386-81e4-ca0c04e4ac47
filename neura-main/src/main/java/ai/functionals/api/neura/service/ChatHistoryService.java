package ai.functionals.api.neura.service;

import ai.functionals.api.neura.jpa.entity.SoftwareDesignerVersion;
import ai.functionals.api.neura.jpa.repo.SoftwareDesignerVersionRepo;
import ai.functionals.api.neura.model.commons.AppUser;
import ai.functionals.api.neura.model.rsp.ChatHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {
    private final SoftwareDesignerVersionRepo softwareDesignerVersionRepo;
    private final StorageService storageService;

    private final PageRequest defaultPage = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdTime"));

    @Value("${app.s3.bucket.documents}")
    private String docsBucket;

    public ChatHistoryResponse getChatHistory(AppUser user, String sessionId, String sectionPath, Pageable pageable) {
        log.info("Retrieving chat history for session ID: {}", sessionId);

        Page<SoftwareDesignerVersion> byOwnerIdAndSessionId = StringUtils.isBlank(sectionPath) ?
                softwareDesignerVersionRepo.findByOwnerIdAndSessionId(user.getId(), sessionId, pageable == null ? defaultPage : pageable)
                : softwareDesignerVersionRepo.findByOwnerIdAndSessionIdAndSectionPathsContaining(user.getId(), sessionId, sectionPath, pageable == null ? defaultPage : pageable);

        ChatHistoryResponse chatHistoryResponse = ChatHistoryResponse.builder().build();
        chatHistoryResponse.setSessionId(sessionId);
        List<ChatHistoryResponse.ChatDiff> chatDiffs = byOwnerIdAndSessionId.getContent().stream().map(history -> ChatHistoryResponse.ChatDiff.builder()
                .previous(storageService.getDoc(docsBucket, user.getTenantId(), sessionId, history.getPreviousDocumentVersion()))
                .current(storageService.getDoc(docsBucket, user.getTenantId(), sessionId, history.getCurrentDocumentVersion()))
                .prompt(history.getPrompt())
                .version(history.getCurrentDocumentVersion())
                .llmResponse(history.getLlmResponseDescription())
                .sectionPath(history.getSectionPaths())
                .updateTime(history.getCreatedTime())
                .build()).toList();
        chatHistoryResponse.setChatDiffs(new PageImpl<>(chatDiffs, byOwnerIdAndSessionId.getPageable(), byOwnerIdAndSessionId.getTotalElements()));
        log.info("Chat history retrieved successfully for session ID: {}", sessionId);
        return chatHistoryResponse;
    }
}
