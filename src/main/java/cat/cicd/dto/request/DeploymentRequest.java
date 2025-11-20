package cat.cicd.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeploymentRequest {
    @NotBlank
    private String serviceName;
    @NotBlank
    private String clusterName;
    @NotBlank
    private String containerName;
    @NotBlank
    private String imageTag;
}
