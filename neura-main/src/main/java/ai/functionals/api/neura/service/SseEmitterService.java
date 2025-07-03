package ai.functionals.api.neura.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();


    public void cleanupEmitter(String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error completing emitter for session: {}", sessionId, e);
            }
        }
    }

    public void send(String sessionId, Object response) {
        send(sessionId, null, response);
    }

    public void send(String sessionId, Long delay, Object response) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                if (delay != null && delay > 0 && delay < 2000) Thread.sleep(delay);
                emitter.send(response);
            } catch (Exception e) {
                log.error("Failed to send response to user for session: {}", sessionId, e);
                cleanupEmitter(sessionId);
            }
        } else {
            log.warn("No emitter found for session: {}", sessionId);
        }
    }

    public SseEmitter subscribeToLLMEvents(String sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for session: {}", sessionId);
            cleanupEmitter(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out for session: {}", sessionId);
            cleanupEmitter(sessionId);
        });
        emitter.onError((e) -> {
            log.error("SSE connection error for session: {} - {}", sessionId, e.getMessage());
            cleanupEmitter(sessionId);
        });
        emitters.put(sessionId, emitter);
        return emitter;
    }
}
