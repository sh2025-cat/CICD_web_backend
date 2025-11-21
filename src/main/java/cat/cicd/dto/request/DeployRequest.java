package cat.cicd.dto.request;

import jakarta.validation.constraints.NotNull;

public record DeployRequest(@NotNull String imageTag, Long deploymentId) {
}
