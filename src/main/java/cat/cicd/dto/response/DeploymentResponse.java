package cat.cicd.dto.response;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.Deployment.DeploymentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DeploymentResponse(
		Long id,
		ProjectResponse project,
		ArtifactResponse artifact,
		DeploymentStatus status,
		String taskDefinitionArn,
		Long previousDeploymentId,
		LocalDateTime createdAt
) {
	public static DeploymentResponse from(Deployment deployment) {
		if (deployment == null)
			return null;

		return DeploymentResponse.builder()
				.id(deployment.getId())
				.project(ProjectResponse.from(deployment.getProject()))
				.artifact(ArtifactResponse.from(deployment.getArtifact()))
				.status(deployment.getStatus())
				.taskDefinitionArn("")
				.previousDeploymentId(0L)
				.createdAt(deployment.getCreatedAt())
				.build();
	}
}
