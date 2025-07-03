package ai.functionals.api.neura.controller;

import ai.functionals.api.neura.jpa.entity.SoftwareDesigner;
import ai.functionals.api.neura.model.req.LLMRequest;
import ai.functionals.api.neura.model.rsp.LLMResponse;
import ai.functionals.api.neura.service.SoftwareDesignerService;
import ai.functionals.api.neura.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/designer")
@RequiredArgsConstructor
public class SoftwareDesignerController implements BaseController {
    private final SoftwareDesignerService softwareDesignerService;
    private final SseEmitterService sseEmitterService;
    private final PDFTextStripper pdfStripper;

    @PostMapping(value = "/begin", consumes = {"multipart/form-data"})
    public String beginWithIdea(@RequestParam(value = "file", required = false) MultipartFile file,
                                @RequestParam String prompt) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        String filePrompt = null;

        if (file != null) {
            log.info("Begin request received with file: {}", file.getOriginalFilename());
            if (StringUtils.equalsIgnoreCase(file.getContentType(), "application/pdf")) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    filePrompt = pdfStripper.getText(document);
                } catch (IOException e) {
                    log.error("Error reading PDF file: {}", e.getMessage());
                }
            } else {
                filePrompt = new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            if (StringUtils.isNotBlank(filePrompt)) {
                // tabs (\x09) | line feeds (\x0A) | carriage returns (\x0D) | from space (0x20) through tilde (0x7E)
                filePrompt = filePrompt.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", "");
            }
        }

        if (StringUtils.isNotBlank(filePrompt) || StringUtils.isNotBlank(prompt)) {
            log.info("Calling LLM for session: {} with initial prompt: {}", sessionId, prompt);
            softwareDesignerService.beginWithIdea(prompt + "\n" + filePrompt, getCurrentUser(), sessionId);
        }
        return sessionId;
    }

    @GetMapping(value = "/{sessionId}/events")
    public SseEmitter sessionIdToSse(@PathVariable String sessionId) {
        log.info("Events request received for sessionId {}", sessionId);
        return sseEmitterService.subscribeToLLMEvents(sessionId);
    }

    @PostMapping(value = "/{sessionId}/clarification")
    public ResponseEntity<String> submitClarificationsPrompt(@PathVariable String sessionId, @RequestBody String prompt) {
        log.info("Clarification request received with prompt {}", prompt);
        if (prompt != null && !prompt.isEmpty()) {
            softwareDesignerService.submitClarificationsPrompt(prompt, getCurrentUser(), sessionId);
        }
        return ResponseEntity.ok("accepted");
    }


    @PostMapping(value = {"/{sessionId}/feedback", "/{sessionId}/feedback/{feedbackId}"})
    public ResponseEntity<String> submitFeedbackPrompt(@PathVariable String sessionId, @PathVariable(required = false) String feedbackId,
                                                       @RequestBody LLMRequest request) {
        log.info("Feedback request received : {}", request.getPrompt());
        softwareDesignerService.submitFeedbackPrompt(request, getCurrentUser(), sessionId, feedbackId);
        return ResponseEntity.ok("accepted");
    }

    @GetMapping(value = "/{sessionId}/current")
    public ResponseEntity<LLMResponse> getGurrentData(@PathVariable String sessionId) {
        return ResponseEntity.ok(softwareDesignerService.getCurrentData(getCurrentUser(), sessionId));
    }

    @PostMapping(value = "/{sessionId}/rollback/{version}")
    public ResponseEntity<?> rollbackHistory(@PathVariable String sessionId, @PathVariable Integer version) {
        softwareDesignerService.rollback(getCurrentUser(), sessionId, version);
        return ResponseEntity.ok("success");
    }


    @GetMapping("/{sessionId}")
    public SoftwareDesigner findBySessionId(@PathVariable String sessionId) {
        return softwareDesignerService.findBySessionId(getCurrentUser(), sessionId);
    }

    @GetMapping
    public Page<SoftwareDesigner> findAll(Pageable pageable) {
        return softwareDesignerService.findAll(getCurrentUser(), pageable);
    }

    @PutMapping("/{sessionId}")
    public SoftwareDesigner update(@PathVariable String sessionId, @RequestBody SoftwareDesigner designer) {
        designer.setSessionId(sessionId);
        return softwareDesignerService.update(getCurrentUser(), designer);
    }

    @DeleteMapping("/{sessionId}")
    public void delete(@PathVariable String sessionId) {
        softwareDesignerService.deleteById(getCurrentUser(), sessionId);
    }
}
