package cat.cicd.controller;

import cat.cicd.dto.request.GithubActionStepRequest;
import cat.cicd.service.GithubActionService;
import io.swagger.v3.oas.annotations.Operation;
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
			@RequestHeader("X-GitHub-Event") String eventType,
			@RequestBody Map<String, Object> payload
	) {
		if("workflow_job".equals(eventType)) {
			Map<String, Object> job = (Map<String, Object>) payload.get("workflow_job");
			String status = (String) job.get("status");
      // conclusion can be null
			Object conclusion = job.get("conclusion");

			log.info("Job Status Changed: status - {}, conclusion - {}", status, conclusion);
		}

		return ResponseEntity.ok().build();
	}

	@Operation(summary = "GitHub Actions Step 웹훅 수신", description = "GitHub Actions의 각 Step 진행 상황에 대한 웹훅을 받아 처리합니다.")
	@PostMapping("/github/step")
	public ResponseEntity<Void> handleGithubActionStepWebhook(@RequestBody GithubActionStepRequest request) {
		githubActionService.processStepWebhook(request);
		return ResponseEntity.ok().build();
	}
}
