package cat.cicd.controller;

import cat.cicd.dto.request.ProjectRequest;
import cat.cicd.dto.response.ProjectResponse;
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
    @PostMapping("/projects/{projectName}/discover-ecs")
    public ResponseEntity<CommonResponse<ProjectResponse>> discoverAndSaveEcsInfo(@PathVariable String projectName) {
        Project updatedProject = ecsService.discoverAndSaveEcsInfo(projectName);
        return ResponseEntity.ok(CommonResponse.of(ProjectResponse.from(updatedProject), "ECS info discovered and saved."));
    }

    @Operation(summary = "모든 ECS 클러스터 목록 조회", description = "AWS 계정의 모든 ECS 클러스터 ARN 목록을 조회합니다.")
    @GetMapping("/ecs/clusters")
    public ResponseEntity<CommonResponse<List<String>>> listEcsClusters() {
        List<String> clusters = ecsService.listClusters();
        return ResponseEntity.ok(CommonResponse.of(clusters));
    }

    @Operation(summary = "특정 클러스터의 서비스 목록 조회", description = "지정된 ECS 클러스터의 모든 서비스 ARN 목록을 조회합니다.")
    @GetMapping("/ecs/clusters/{projectId}/services")
    public ResponseEntity<CommonResponse<List<String>>> listEcsServices(@PathVariable long projectId) {
        List<String> services = ecsService.listServices(projectId);
        return ResponseEntity.ok(CommonResponse.of(services));
    }
}
