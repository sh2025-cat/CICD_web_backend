package cat.cicd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String serviceName) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String emitterId = serviceName;

        emitter.onCompletion(() -> {
            log.info("Emitter completed for {}", emitterId);
            this.emitters.remove(emitterId);
        });
        emitter.onTimeout(() -> {
            log.info("Emitter timed out for {}", emitterId);
            emitter.complete();
            this.emitters.remove(emitterId);
        });
        emitter.onError(e -> {
            log.error("Emitter error for {}: {}", emitterId, e.getMessage());
            this.emitters.remove(emitterId);
        });

        this.emitters.put(emitterId, emitter);

        try {
            emitter.send(SseEmitter.event().name("connect").data("Connection established for " + serviceName));
        } catch (IOException e) {
            log.error("Failed to send initial connection message to {}: {}", emitterId, e.getMessage());
            emitter.completeWithError(e);
        }

        log.info("New emitter subscribed: {}", emitterId);
        return emitter;
    }

    public void send(String serviceName, String eventName, Object data) {
        SseEmitter emitter = emitters.get(serviceName);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                log.info("Sent event '{}' to emitter '{}'", eventName, serviceName);
            } catch (IOException e) {
                log.error("Failed to send event to emitter '{}': {}", serviceName, e.getMessage());
                emitter.completeWithError(e);
            }
        } else {
            log.warn("No active emitter found for service '{}'", serviceName);
        }
    }
}
