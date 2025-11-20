package cat.cicd.dto.response;

import cat.cicd.entity.Deployment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeploymentResponse {
    private final Long id;
    private final ServiceResponse service;
    private final ArtifactResponse artifact;
    private final Deployment.DeploymentStatus status;
    private final String taskDefinitionArn;
    private final Long previousDeploymentId;
    private final LocalDateTime createdAt;

    public static DeploymentResponse from(Deployment deployment) {
        if (deployment == null) return null;

        Long prevId = deployment.getPreviousDeployment() != null ? deployment.getPreviousDeployment().getId() : null;

        return DeploymentResponse.builder()
                .id(deployment.getId())
                .service(ServiceResponse.from(deployment.getProject()))
                .artifact(ArtifactResponse.from(deployment.getArtifact()))
                .status(deployment.getStatus())
                .taskDefinitionArn(deployment.getTaskDefinitionArn())
                .previousDeploymentId(prevId)
                .createdAt(deployment.getCreatedAt())
                .build();
    }
}
