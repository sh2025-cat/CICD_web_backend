package cat.cicd.service;

import cat.cicd.dto.response.DeploymentLogResponse;
import cat.cicd.dto.response.WorkflowInfoResponse;
import cat.cicd.entity.Deployment;
import cat.cicd.entity.DeploymentLog;
import cat.cicd.repository.DeploymentLogRepository;
import cat.cicd.repository.DeploymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class GithubActionService {

	private final String BASE_URL = "https://api.github.com";

	@Value("${github.access-token}")
	private String GITHUB_TOKEN;

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	private final RestClient restClient;
	private final SseService sseService;
	private final DeploymentRepository deploymentRepository;
	private final DeploymentLogRepository deploymentLogRepository;

	public GithubActionService(RestClient restClient, SseService sseService, DeploymentRepository deploymentRepository,
			DeploymentLogRepository deploymentLogRepository) {
		this.restClient = restClient;
		this.sseService = sseService;
		this.deploymentRepository = deploymentRepository;
		this.deploymentLogRepository = deploymentLogRepository;
	}

	@Transactional
	public void handleWorkflowJobWebhook(Map<String, Object> payload) {
		Map<String, Object> job = (Map<String, Object>) payload.get("workflow_job");
		Long runId = ((Number) job.get("run_id")).longValue();
		Long jobId = ((Number) job.get("id")).longValue();
		String jobName = (String) job.get("name");
		String status = (String) job.get("status");
		String conclusion = (String) job.get("conclusion");

		Deployment deployment = deploymentRepository.findByRunId(runId)
				.orElseThrow(() -> new EntityNotFoundException("Deployment not found for runId: " + runId));

		DeploymentLog step = deploymentLogRepository.findByDeploymentAndStepName(deployment, jobName)
				.orElseGet(() -> new DeploymentLog(deployment, jobId, jobName, status));

		step.setStatus(status);
		if ("in_progress".equals(status) && step.getStartedAt() == null) {
			step.setStartedAt(LocalDateTime.now());
		}
		if ("completed".equals(status)) {
			step.setCompletedAt(LocalDateTime.now());
		}
		deploymentLogRepository.save(step);

		if ("failure".equals(conclusion)) {
			deployment.setStatus(Deployment.DeploymentStatus.FAILED);
		}

		DeploymentLogResponse stepResponse = DeploymentLogResponse.from(step);
		// 프론트가 알기 쉬운 형태(예: "stages" 구조)로 매핑해서 보내주면 더 좋습니다.

		sseService.send(deployment.getProject().getName(), "deployment_update", stepResponse);
	}


	/**
	 * Repository 내 모든 파이프라인 실행 리스트를 가져온다.
	 * @param owner GitHub User / Organization
	 * @param repo Repository Name
	 * @return Json 타입의 결과 ({@link WorkflowInfoResponse}로 반환 예정)
	 */
	public JsonNode getPipelinesInRepository(String owner, String repo) {
		return restClient
				.get()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs")
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.retrieve()
				.body(JsonNode.class);
	}

	/**
	 * 특정 Run 내에 실행된 모든 Job 리스트를 조회한다.
	 * @param owner GitHub User / Organization
	 * @param repo Repository Name
	 * @param runId Run ID
	 * @return Json 타입의 결과 ( 반환 예정)
	 */
	public JsonNode getJobList(String owner, String repo, long runId) {
		return restClient
				.get()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs")
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.retrieve()
				.body(JsonNode.class);
	}

	public String getJobLogs(String owner, String repo, long jobId) {
		URI downloadUrl = getLogDownloadUrl(owner, repo, jobId);

		return RestClient.create()
				.get()
				.uri(downloadUrl)
				.retrieve()
				.body(String.class);
	}

	public byte[] getRunLogs(String owner, String repo, long runId) {
		return restClient
				.get()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/logs")
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.retrieve()
				.body(byte[].class);
	}

	public JsonNode getJobInfo(String owner, String repo, long jobId) {
		return restClient
				.get()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/jobs/" + jobId)
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.retrieve()
				.body(JsonNode.class);
	}

	public void startPipeline(String owner, String repo, long workflowId, String branchName) {
		Map<String, String> body = Map.of(
				"ref", branchName
		);

		restClient.post()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/workflows/" + workflowId + "/dispatches")
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity();
	}

	public void cancelPipeline(String owner, String repo, long runId) {
		restClient.post()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/cancel")
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.retrieve()
				.toBodilessEntity();
	}

	public void rerunPipeline(String owner, String repo, long runId) {
		restClient.post()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/rerun")
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.retrieve()
				.toBodilessEntity();
	}

	public void streamLogsToEmitter(SseEmitter emitter, String owner, String repo, long jobId) {
		executor.execute(() -> {
			try {
				URI rawLocation = getLogDownloadUrl(owner, repo, jobId);

				RestClient.create()
						.get()
						.uri(rawLocation)
						.exchange((request, response) -> {
							InputStream inputStream = response.getBody();

							try (BufferedReader reader = new BufferedReader(
									new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

								String line;
								while ((line = reader.readLine()) != null) {
									emitter.send(SseEmitter.event().id(String.valueOf(System.currentTimeMillis())).name("log").data(line));
								}

								emitter.send(SseEmitter.event().name("complete").data("End of Log"));
								emitter.complete();
							}
							return null;
						});

			} catch (Exception e) {
				log.error("Log Streaming Failed", e);
				emitter.completeWithError(e);
			}
		});
	}

	/**
	 * Git Action의 로그를 다운로드 받을 URL을 가져온다.
	 * @param owner GitHub User / Organization
	 * @param repo Repository Name
	 * @param jobId jobId
	 * @return 다운로드 URL
	 */
	private URI getLogDownloadUrl(String owner, String repo, long jobId) {
		return restClient.get()
				.uri(BASE_URL + "/repos/" + owner + "/" + repo + "/actions/jobs/" + jobId + "/logs")
				.header("Authorization", "Bearer " + GITHUB_TOKEN)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.exchange((request, response) -> {
					if (response.getStatusCode().is3xxRedirection()) {
						return response.getHeaders().getLocation();
					} else {
						throw new RuntimeException("Failed to get log URL. Status: " + response.getStatusCode());
					}
				});
	}
}
