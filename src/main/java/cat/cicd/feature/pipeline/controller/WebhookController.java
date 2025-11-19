package cat.cicd.feature.pipeline.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

	@PostMapping("/github")
	public ResponseEntity<Void> handleGithubWebhook(
			@RequestHeader("X-GitHub-Event") String eventType,
			@RequestBody Map<String, Object> payload
	) {
		if("workflow_job".equals(eventType)) {
			Map<String, Object> job = (Map<String, Object>) payload.get("workflow_job");
			String status = (String) job.get("status");
			String conclusion = (String) job.get("conclusion");

			log.info("Job Status Changed: {} - {}", status, conclusion);
		}

		return ResponseEntity.ok().build();
	}
}
