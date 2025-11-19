package cat.cicd.feature.pipeline.controller;

import cat.cicd.feature.pipeline.dto.GitHubBaseRequest;
import cat.cicd.feature.pipeline.service.GithubActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubLogController {

	private final GithubActionService githubActionService;

	// 테스트 curl -N "http://localhost:8080/api/jobs/12345/logs/stream?owner=SonJM&repo=deploy-test"
	@Operation(summary = "Git Action 로그 실시간 스트리밍 (SSE)",
			description = "GitHub Action Job의 실행 로그를 한 줄씩 실시간으로 반환합니다. (EventSource 연결 필요)")
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "스트림 연결 성공",
					content = @Content(
							mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
							schema = @Schema(type = "string", example = "2025-11-19T12:00:00Z [INFO] Build Started...")
					)
			),
			@ApiResponse(responseCode = "404", description = "Job을 찾을 수 없음", content = @Content),
			@ApiResponse(responseCode = "500", description = "GitHub API 연동 실패", content = @Content)
	})
	@GetMapping(value = "jobs/{jobId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamJobLogs(
			@ModelAttribute GitHubBaseRequest req,
			@PathVariable long jobId) {

		SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

		githubActionService.streamLogsToEmitter(emitter, req.owner(), req.repo(), jobId);

		return emitter;
	}
}
