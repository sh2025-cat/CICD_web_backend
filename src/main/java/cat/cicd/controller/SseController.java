package cat.cicd.controller;

import cat.cicd.dto.request.GitHubBaseRequest;
import cat.cicd.service.GithubActionService;
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

	@Operation(summary = "Git Action 로그 실시간 스트리밍", description = "GitHub Action Job의 실행 로그를 한 줄씩 실시간으로 반환합니다. (EventSource 연결 필요)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "스트림 연결 성공", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string", example = """
					id:1763540877498
					event:log
					data:\uFEFF2025-11-17T08:40:48.0337571Z Current runner version: '2.329.0'
					"""))) })
	@GetMapping(value = "/jobs/{jobId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamJobLogs(@ModelAttribute GitHubBaseRequest req, @PathVariable long jobId) {

		SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

		githubActionService.streamLogsToEmitter(emitter, req.owner(), req.repo(), jobId);

		return emitter;
	}
}
