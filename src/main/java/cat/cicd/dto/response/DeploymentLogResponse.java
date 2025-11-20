package cat.cicd.dto.response;

import cat.cicd.entity.DeploymentLog;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeploymentLogResponse(
		Long id,
		Long pipelineId,
		String name,
		String status,
		String log,
		LocalDateTime startedAt,
		LocalDateTime completedAt
) {
	public static DeploymentLogResponse from(DeploymentLog step) {
		if (step == null) return null;

		return DeploymentLogResponse.builder()
				.id(step.getId())
				.pipelineId(step.getDeployment().getId())
				.name(step.getStepName())
				.status(step.getStatus())
				.log(step.getLogContent())
				.startedAt(step.getStartedAt())
				.completedAt(step.getCompletedAt())
				.build();
	}
}
