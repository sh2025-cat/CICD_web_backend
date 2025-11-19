package cat.cicd.feature.deployment.controller;

import cat.cicd.feature.deployment.dto.EcsDeployRequest;
import cat.cicd.feature.deployment.service.EcsService;
import cat.cicd.global.common.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.ecs.model.Service;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final EcsService ecsService;

    /**
     * AWS ECS 서비스에 새로운 이미지를 배포합니다.
     *
     * @param request 배포 요청 정보
     * @return 배포 요청 결과
     */
    @PostMapping("/ecs")
    public ResponseEntity<CommonResponse> deployToEcs(
			@Valid @RequestBody EcsDeployRequest request
	) {
		Service updatedService = ecsService.updateService(request);
        return ResponseEntity.ok(
				new CommonResponse(
						HttpStatus.OK,
						"ECS 배포 요청에 성공했습니다.",
						updatedService.serviceName()
				)
		);
    }

    // Todo: ECS 배포 롤백 API 추가 필요
}
