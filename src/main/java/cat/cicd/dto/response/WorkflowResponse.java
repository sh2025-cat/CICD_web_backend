package cat.cicd.dto.response;

import cat.cicd.entity.WorkFlow;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WorkflowResponse {
    private final Long id;
    private final String githubRunId;
    private final String status;
    private final Long serviceId;
    private final Long deploymentId;
    private final LocalDateTime createdAt;

    public static WorkflowResponse from(WorkFlow workFlow) {
        if (workFlow == null) return null;
        return WorkflowResponse.builder()
                .id(workFlow.getId())
                .githubRunId(workFlow.getGithubRunId())
                .status(workFlow.getStatus())
                .serviceId(workFlow.getProject().getId())
                .deploymentId(workFlow.getDeployment() != null ? workFlow.getDeployment().getId() : null)
                .createdAt(workFlow.getCreatedAt())
                .build();
    }
}
