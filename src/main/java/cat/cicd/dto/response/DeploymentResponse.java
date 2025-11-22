package cat.cicd.dto.response;

import cat.cicd.entity.Deployment;
import cat.cicd.global.enums.ProgressStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DeploymentResponse(Long deploymentId, ProgressStatus status, String taskDefinitionArn, LocalDateTime createdAt) {
	public static DeploymentResponse from(Deployment deployment) {
		if (deployment == null)
			return null;

		return DeploymentResponse.builder()
				.deploymentId(deployment.getId())
				.status(deployment.getPipelineStatus())
				.taskDefinitionArn(deployment.getTaskDefinitionArn())
				.createdAt(deployment.getCreatedAt())
				.build();
	}
}
