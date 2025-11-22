package cat.cicd.controller;

import cat.cicd.service.GithubActionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacUtils; // Apache Commons Codec 라이브러리 사용 시 편리함
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ci-webhook")
public class WebhookController {

    @Value("${github.webhook-secret}")
    private String webhookSecret;

    private final ObjectMapper objectMapper;
    private final GithubActionService githubActionService;

    public WebhookController(ObjectMapper objectMapper, GithubActionService githubActionService) {
        this.objectMapper = objectMapper;
        this.githubActionService = githubActionService;
    }

    @PostMapping
    public ResponseEntity<Void> handleGithubWebhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "unknown") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256") String signature,
            @RequestBody String rawPayload) {

        if (!isSignatureValid(rawPayload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<>() {});

            if (eventType.equals("workflow_job")) {
                githubActionService.handleWorkflowJobWebhook(payload);
            } else if (eventType.equals("workflow_run")) {
                githubActionService.handleWorkflowRunWebhook(payload);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean isSignatureValid(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        String algorithm = "HmacSHA256";
        String computedSignature = "sha256=" + new HmacUtils(algorithm, webhookSecret).hmacHex(payload);

        return computedSignature.equals(signature);
    }
}
