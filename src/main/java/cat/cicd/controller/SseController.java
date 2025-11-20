package cat.cicd.controller;

import cat.cicd.dto.request.GitHubBaseRequest;
import cat.cicd.service.GithubActionService;
import cat.cicd.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE Controller", description = "실시간 알림을 위한 Server-Sent Events API")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

	private final GithubActionService githubActionService;
    private final SseService sseService;

    @Operation(summary = "SSE 구독", description = "특정 서비스의 파이프라인 이벤트에 대한 실시간 알림을 구독합니다.")
    @GetMapping(value = "/subscribe/{serviceName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String serviceName) {
        return sseService.subscribe(serviceName);
    }

	// 테스트 curl -N "http://localhost:8080/api/jobs/12345/logs/stream?owner=SonJM&repo=deploy-test"
	@Operation(summary = "Git Action 로그 실시간 스트리밍", description = "GitHub Action Job의 실행 로그를 한 줄씩 실시간으로 반환합니다. (EventSource 연결 필요)")
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
	@GetMapping(value = "/jobs/{jobId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamJobLogs(
			@ModelAttribute GitHubBaseRequest req,
			@PathVariable long jobId) {

		SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

		githubActionService.streamLogsToEmitter(emitter, req.owner(), req.repo(), jobId);

		return emitter;
	}
}
