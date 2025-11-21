package cat.cicd.dto.request;

import jakarta.validation.constraints.NotNull;

public record DeployRequest(@NotNull Long projectId, @NotNull String imageTag, Long deploymentId) {
}
