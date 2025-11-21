package cat.cicd.controller;

import cat.cicd.dto.request.GitHubBaseRequest;
import cat.cicd.global.common.CommonResponse;
import cat.cicd.service.GithubActionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

	private final GithubActionService gitHubActionService;

	@Deprecated
	@Operation(summary = "파이프라인 목록 조회 API", description = "해당 레포지토리의 전체 파이프라인을 조회해오는 API 입니다.")
	@GetMapping()
	public ResponseEntity<CommonResponse> getPipelines(@RequestBody GitHubBaseRequest req) {
		return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "pipelines list",
				gitHubActionService.getPipelinesInRepository(req.owner(), req.repo())));
	}

	@Deprecated
	@Operation(summary = "파이프라인 상세 조회 API", description = "특정 파이프라인 내의 모든 Job을 조회해오는 API 입니다.")
	@GetMapping("{runId}/jobs")
	public ResponseEntity<CommonResponse> getJobList(@PathVariable long runId, @RequestBody GitHubBaseRequest req) {
		return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "jobs list",
				gitHubActionService.getJobList(req.owner(), req.repo(), runId)));
	}

	@Deprecated
	@Operation(summary = "Job 상세 조회 API", description = "특정 Job에 대한 상세 정보를 조회하는 API 입니다.")
	@GetMapping("job/{jobId}")
	public ResponseEntity<CommonResponse> getJobInfo(@PathVariable long jobId, @RequestBody GitHubBaseRequest req) {
		return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "jobs list",
				gitHubActionService.getJobInfo(req.owner(), req.repo(), jobId)));
	}

	@Operation(summary = "Job 로그 조회 API", description = "특정 Job에 실행 로그를 반환하는 API 입니다.")
	@GetMapping("{jobId}/jobs/logs")
	public ResponseEntity<CommonResponse> getJobsLogs(@PathVariable long jobId, @RequestBody GitHubBaseRequest req) {
		return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "Get Job's Log",
				gitHubActionService.getJobLogs(req.owner(), req.repo(), jobId)));
	}

	@Deprecated
	@Operation(summary = "특정 Pipeline 실행", description = "특정 workflow 실행 명령 API 입니다.")
	@PostMapping("{workflowId}/start")
	public ResponseEntity<Void> runJob(@PathVariable long workflowId,
			@RequestParam(defaultValue = "main") String branch, @RequestBody GitHubBaseRequest req) {
		gitHubActionService.startPipeline(req.owner(), req.repo(), workflowId, branch);
		return ResponseEntity.ok().build();
	}

	@Deprecated
	@Operation(summary = "특정 Pipeline 정지", description = "특정 workflow 정지 명령 API 입니다.")
	@PostMapping("{runId}/cancel")
	public ResponseEntity<CommonResponse> cancelJob(@PathVariable long runId, @RequestBody GitHubBaseRequest req) {
		gitHubActionService.cancelPipeline(req.owner(), req.repo(), runId);
		return ResponseEntity.ok().build();
	}

	@Deprecated
	@Operation(summary = "특정 Pipeline 재실행", description = "특정 workflow 재실행 명령 API 입니다.")
	@PostMapping("{runId}/rerun")
	public ResponseEntity<CommonResponse> rerunJob(@PathVariable long runId, @RequestBody GitHubBaseRequest req) {
		gitHubActionService.rerunPipeline(req.owner(), req.repo(), runId);
		return ResponseEntity.ok().build();
	}
}
