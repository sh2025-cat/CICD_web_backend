package cat.cicd.controller;

import cat.cicd.dto.request.DeployRequest;
import cat.cicd.dto.response.DeploymentResponse;
import cat.cicd.entity.Deployment;
import cat.cicd.global.common.CommonResponse;
import cat.cicd.service.ECSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Deployment Controller", description = "ECS 배포 및 롤백 관련 API")
@RestController
@RequestMapping("/api/ecs")
@RequiredArgsConstructor
public class EcsController {

	private final ECSService ecsService;

	@Deprecated
	@Operation(summary = "모든 ECS 클러스터 목록 조회", description = "AWS 계정의 모든 ECS 클러스터 ARN 목록을 조회합니다.")
	@GetMapping("/clusters")
	public ResponseEntity<CommonResponse<List<String>>> listEcsClusters() {
		List<String> clusters = ecsService.listClusters();
		return ResponseEntity.ok(CommonResponse.of(clusters));
	}

	@Deprecated
	@Operation(summary = "특정 클러스터의 서비스 목록 조회", description = "지정된 ECS 클러스터의 모든 서비스 ARN 목록을 조회합니다.")
	@GetMapping("/clusters/{projectId}/services")
	public ResponseEntity<CommonResponse<List<String>>> listEcsServices(@PathVariable long projectId) {
		List<String> services = ecsService.listServices(projectId);
		return ResponseEntity.ok(CommonResponse.of(services));
	}

	@Operation(summary = "ECS 신규 버전 배포", description = "지정된 태그의 새 이미지로 ECS 서비스를 배포합니다.")
	@PostMapping("/deploy")
	public ResponseEntity<CommonResponse<DeploymentResponse>> deploy(@Valid @RequestBody DeployRequest request) {
		Deployment deployment = ecsService.deployNewVersion(request.projectId(), request.imageTag());
		return ResponseEntity.ok(CommonResponse.of(DeploymentResponse.from(deployment)));
	}

	@Operation(summary = "ECS 이전 버전 롤백", description = "ECS 서비스를 가장 마지막으로 성공한 버전으로 롤백합니다.")
	@PostMapping("/rollback/{deploymentId}")
	public ResponseEntity<CommonResponse<DeploymentResponse>> rollback(@PathVariable long deploymentId) {
		Deployment rollbackDeployment = ecsService.rollbackToPreviousVersion(deploymentId);
		return ResponseEntity.ok(CommonResponse.of(DeploymentResponse.from(rollbackDeployment)));
	}
}
