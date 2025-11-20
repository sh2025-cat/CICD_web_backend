package cat.cicd.controller;

import cat.cicd.service.GithubActionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Webhook Controller", description = "외부 서비스(GitHub, etc.)로부터 웹훅을 수신하는 API")
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

	private final GithubActionService githubActionService;

	@PostMapping("/github")
	public ResponseEntity<Void> handleGithubWebhook(
			@RequestHeader(value = "X-GitHub-Event", defaultValue = "unknown") String eventType,
			@RequestBody Map<String, Object> payload
	) {
		if ("workflow_job".equals(eventType)) {
			githubActionService.handleWorkflowJobWebhook(payload);
		}

		return ResponseEntity.ok().build();
	}
}
