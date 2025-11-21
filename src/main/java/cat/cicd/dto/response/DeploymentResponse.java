package cat.cicd.dto.response;

import cat.cicd.entity.Deployment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DeploymentResponse(Long deploymentId, String status, String taskDefinitionArn, LocalDateTime createdAt) {
	public static DeploymentResponse from(Deployment deployment) {
		if (deployment == null)
			return null;

		return DeploymentResponse.builder()
				.deploymentId(deployment.getId())
				.status(deployment.getStatus().name())
				.taskDefinitionArn(deployment.getTaskDefinitionArn())
				.createdAt(deployment.getCreatedAt())
				.build();
	}
}
