package cat.cicd.service;

import cat.cicd.dto.request.GithubActionStepRequest;
import cat.cicd.dto.response.WorkflowStepResponse;
import cat.cicd.dto.response.WorkflowInfoResponse;
import cat.cicd.entity.WorkFlow;
import cat.cicd.entity.WorkflowJob;
import cat.cicd.entity.Project;
import cat.cicd.repository.WorkflowRepository;
import cat.cicd.repository.WorkflowStepRepository;
import cat.cicd.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubActionService {

	private final RestClient restClient;
	private final ProjectRepository projectRepository;
	private final WorkflowRepository workflowRepository;
	private final WorkflowStepRepository workflowStepRepository;
	private final SseService sseService;
	private final String BASE_URL = "https://api.github.com";
	@Value("${github.access-token}")
	private String GITHUB_TOKEN;

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	@Transactional
	public void processStepWebhook(GithubActionStepRequest request) {
		log.info("Processing webhook request: {}", request);

		Project project = projectRepository.findByName(request.getRepositoryName())
				.orElseThrow(() -> new IllegalArgumentException("Service not found with name: " + request.getRepositoryName()));

		WorkFlow workFlow = workflowRepository.findByGithubRunId(request.getGithubRunId())
				.orElseGet(() -> {
					WorkFlow newWorkFlow = new WorkFlow(request.getGithubRunId(), "IN_PROGRESS", project);
					return workflowRepository.save(newWorkFlow);
				});

		String stepIdentifier = request.getJobName() + " / " + request.getStepName();
		WorkflowJob workflowJob = workflowStepRepository.findByWorkFlowAndName(workFlow, stepIdentifier)
				.orElseGet(() -> {
					WorkflowJob newStep = new WorkflowJob(workFlow, stepIdentifier, request.getStatus());
					return workflowStepRepository.save(newStep);
				});

		workflowJob.setStatus(request.getStatus());
		workflowJob.setLog(request.getLog());
		if (request.getStartedAt() != null) {
			workflowJob.setStartedAt(LocalDateTime.parse(request.getStartedAt(), DateTimeFormatter.ISO_DATE_TIME));
		}
		if (request.getCompletedAt() != null) {
			workflowJob.setCompletedAt(LocalDateTime.parse(request.getCompletedAt(), DateTimeFormatter.ISO_DATE_TIME));
		}
		workflowStepRepository.save(workflowJob);

		if ("SUCCESS".equals(request.getStatus()) || "FAILURE".equals(request.getStatus())) {
			workFlow.setStatus(request.getStatus());
			workflowRepository.save(workFlow);
		}


		sseService.send(project.getName(), "pipeline_step_update", WorkflowStepResponse.from(workflowJob));
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
