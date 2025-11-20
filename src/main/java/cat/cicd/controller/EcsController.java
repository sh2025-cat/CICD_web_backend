package cat.cicd.controller;

import cat.cicd.dto.request.DeploymentRequest;
import cat.cicd.dto.response.DeploymentResponse;
import cat.cicd.dto.request.RollbackRequest;
import cat.cicd.entity.Deployment;
import cat.cicd.global.common.CommonResponse;
import cat.cicd.service.ECSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Deployment Controller", description = "ECS 배포 및 롤백 관련 API")
@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class EcsController {

    private final ECSService ecsService;

    @Operation(summary = "ECS 신규 버전 배포", description = "지정된 태그의 새 이미지로 ECS 서비스를 배포합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<DeploymentResponse>> deploy(
            @Valid @RequestBody DeploymentRequest request
    ) {
        Deployment deployment = ecsService.deployNewVersion(
                request.getServiceName(),
                request.getClusterName(),
                request.getContainerName(),
                request.getImageTag()
        );
        return ResponseEntity.ok(CommonResponse.of(DeploymentResponse.from(deployment)));
    }

    @Operation(summary = "ECS 이전 버전 롤백", description = "ECS 서비스를 가장 마지막으로 성공한 버전으로 롤백합니다.")
    @PostMapping("/rollback")
    public ResponseEntity<CommonResponse<DeploymentResponse>> rollback(
            @Valid @RequestBody RollbackRequest request
    ) {
        Deployment rollbackDeployment = ecsService.rollbackToPreviousVersion(
                request.getServiceName(),
                request.getClusterName()
        );
        return ResponseEntity.ok(CommonResponse.of(DeploymentResponse.from(rollbackDeployment)));
    }
}
