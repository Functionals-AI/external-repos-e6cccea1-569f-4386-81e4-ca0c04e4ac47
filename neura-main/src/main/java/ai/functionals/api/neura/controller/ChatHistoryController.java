package ai.functionals.api.neura.controller;

import ai.functionals.api.neura.model.rsp.ChatHistoryResponse;
import ai.functionals.api.neura.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat-history" )
@RequiredArgsConstructor
public class ChatHistoryController implements BaseController {
    private final ChatHistoryService chatHistoryService;

    @GetMapping("/{sessionId}" )
    public ChatHistoryResponse getChatHistory(@PathVariable String sessionId, Pageable pageable) {
        return chatHistoryService.getChatHistory(getCurrentUser(), sessionId, null, pageable);
    }

    @GetMapping("/{sessionId}/section/{sectionPath}" )
    public ChatHistoryResponse getChatHistoryForSection(@PathVariable String sessionId, @PathVariable String sectionPath, PageRequest pageable) {
        return chatHistoryService.getChatHistory(getCurrentUser(), sessionId, sectionPath, pageable);
    }
}
