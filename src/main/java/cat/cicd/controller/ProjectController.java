package cat.cicd.controller;

import cat.cicd.dto.request.ProjectRequest;
import cat.cicd.dto.response.DeploymentDetailResponse;
import cat.cicd.dto.response.DeploymentHistoryResponse;
import cat.cicd.dto.response.ProjectResponse;
import cat.cicd.dto.response.RepoDeployStatusResponse;
import cat.cicd.entity.Project;
import cat.cicd.global.common.CommonResponse;
import cat.cicd.service.ECSService;
import cat.cicd.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Project & ECS Controller", description = "프로젝트 설정 및 ECS 리소스 조회 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectService projectService;
	private final ECSService ecsService;

	@Operation(summary = "새 Repository 추가 API", description = "새로운 프로젝트를 등록하는 API입니다.")
	@PostMapping("/projects")
	public ResponseEntity<?> createNewProject(@Valid @RequestBody ProjectRequest request) {
		projectService.createProject(request);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "ECS 정보 자동 탐색 및 저장", description = "프로젝트 이름과 일치하는 'ProjectRepo' 태그를 가진 ECS 서비스를 찾아 클러스터와 서비스 정보를 저장합니다.")
	@PostMapping("/projects/{projectId}/discover-ecs")
	public ResponseEntity<CommonResponse<ProjectResponse>> discoverAndSaveEcsInfo(@PathVariable long projectId) {
		Project updatedProject = ecsService.discoverAndSaveEcsInfo(projectId);
		return ResponseEntity.ok(
				CommonResponse.of(ProjectResponse.from(updatedProject), "ECS info discovered and saved."));
	}

	@Operation(summary = "레포지토리 별 배포 상태 조회", description = "모든 프로젝트의 최신 배포 상태를 조회합니다.")
	@GetMapping("/repos")
	public ResponseEntity<CommonResponse<List<RepoDeployStatusResponse>>> getRepoDeployStatuses() {
		List<RepoDeployStatusResponse> statuses = projectService.getRepoDeployStatuses();
		return ResponseEntity.ok(CommonResponse.of(statuses));
	}

	@Operation(summary = "배포 상세 조회", description = "특정 배포의 상세 정보를 조회합니다.")
	@GetMapping("/repos/deployment/{deploymentId}")
	public ResponseEntity<CommonResponse<DeploymentDetailResponse>> getDeploymentDetail(
			@PathVariable Long deploymentId) {
		DeploymentDetailResponse detail = projectService.getDeploymentDetail(deploymentId);
		return ResponseEntity.ok(CommonResponse.of(detail));
	}

	@Operation(summary = "레포지토리 배포 이력 조회", description = "특정 프로젝트의 배포 이력을 조회합니다.")
	@GetMapping("/repos/{projectId}/deployments")
	public ResponseEntity<CommonResponse<List<DeploymentHistoryResponse>>> getDeploymentHistory(
			@PathVariable Long projectId) {
		return ResponseEntity.ok(CommonResponse.of(projectService.getDeploymentHistory(projectId)));
	}
}
