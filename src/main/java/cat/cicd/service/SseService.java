package cat.cicd.service;

import cat.cicd.entity.Project;
import cat.cicd.repository.ProjectRepository;
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

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ProjectRepository projectRepository;

    public SseService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public SseEmitter subscribe(long projectId) {
        SseEmitter emitter = new SseEmitter(60 * 1000L * 60);

        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(projectId, emitter));
        emitter.onTimeout(() -> removeEmitter(projectId, emitter));
        emitter.onError((e) -> removeEmitter(projectId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            log.error("Failed to send connect event to emitter '{}': {}", projectId, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void removeEmitter(long projectId, SseEmitter emitter) {
        List<SseEmitter> projectEmitters = emitters.get(projectId);
        if (projectEmitters != null) {
            projectEmitters.remove(emitter);
        }
    }

    public void send(long projectId, String eventName, Object data) {
        List<SseEmitter> projectEmitters = emitters.get(projectId);

        if (projectEmitters != null) {
            projectEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e) {
                    removeEmitter(projectId, emitter);
                }
            });
        }
    }
}
