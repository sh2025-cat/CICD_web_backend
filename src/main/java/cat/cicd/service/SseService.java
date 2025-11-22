package cat.cicd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String projectName) {
        SseEmitter emitter = new SseEmitter(60 * 1000L * 60);

        emitters.computeIfAbsent(projectName, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(projectName, emitter));
        emitter.onTimeout(() -> removeEmitter(projectName, emitter));
        emitter.onError((e) -> removeEmitter(projectName, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            log.error("Failed to send connect event to emitter '{}': {}", projectName, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void removeEmitter(String projectName, SseEmitter emitter) {
        List<SseEmitter> projectEmitters = emitters.get(projectName);
        if (projectEmitters != null) {
            projectEmitters.remove(emitter);
        }
    }

    public void send(String projectName, String eventName, Object data) {
        List<SseEmitter> projectEmitters = emitters.get(projectName);

        if (projectEmitters != null) {
            projectEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e) {
                    removeEmitter(projectName, emitter);
                }
            });
        }
    }
}
