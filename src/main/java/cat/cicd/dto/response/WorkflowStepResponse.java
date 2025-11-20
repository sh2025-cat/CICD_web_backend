package cat.cicd.dto.response;

import cat.cicd.entity.WorkflowJob;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowStepResponse {
    private final Long id;
    private final Long pipelineId;
    private final String name;
    private final String status;
    private final String log;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;

    public static WorkflowStepResponse from(WorkflowJob step) {
        if (step == null) return null;
        return WorkflowStepResponse.builder()
                .id(step.getId())
                .pipelineId(step.getWorkFlow().getId())
                .name(step.getName())
                .status(step.getStatus())
                .log(step.getLog())
                .startedAt(step.getStartedAt())
                .completedAt(step.getCompletedAt())
                .createdAt(step.getCreatedAt())
                .build();
    }
}
