package cat.cicd.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RollbackRequest {
    @NotBlank
    private String serviceName;
    @NotBlank
    private String clusterName;
}
