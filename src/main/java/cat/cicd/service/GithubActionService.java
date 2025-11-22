package cat.cicd.service;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.DeploymentStage;
import cat.cicd.entity.Project;
import cat.cicd.global.enums.ProgressStatus;
import cat.cicd.global.enums.Step;
import cat.cicd.repository.DeploymentRepository;
import cat.cicd.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
	private final DeploymentRepository deploymentRepository;
	private final ProjectRepository projectRepository;
	private final SseService sseService;

	public GithubActionService(DeploymentRepository deploymentRepository, ProjectRepository projectRepository,
			SseService sseService) {
		this.deploymentRepository = deploymentRepository;
		this.projectRepository = projectRepository;
		this.sseService = sseService;
		this.restClient = RestClient.create();
	}

    @Transactional
    public void handleWorkflowJobWebhook(Map<String, Object> payload) {
        Map<String, Object> job = (Map<String, Object>) payload.get("workflow_job");
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");

        if (job == null || repo == null) return;

        String repoName = (String) repo.get("name");
        String repoUrl = (String) repo.get("html_url");
        Map<String, Object> ownerMap = (Map<String, Object>) repo.get("owner");
        String ownerName = (String) ownerMap.get("login");

        String runId = String.valueOf(job.get("run_id"));
        Long jobId = ((Number) job.get("id")).longValue();
        String jobName = (String) job.get("name");
        String status = (String) job.get("status");
        String conclusion = (String) job.get("conclusion");
        String startedAt = (String) job.get("started_at");
        ZonedDateTime utcTime = ZonedDateTime.parse(startedAt);
        ZonedDateTime kstTime = utcTime.withZoneSameInstant(ZoneId.of("Asia/Seoul"));
        LocalDateTime startedAtLocal = kstTime.toLocalDateTime();

        Project project = projectRepository.findByOwnerAndName(ownerName, repoName)
                .orElseGet(() -> projectRepository.save(
                        Project.builder()
                                .owner(ownerName)
                                .name(repoName)
                                .githubRepoUrl(repoUrl)
                                .build()
                ));

        Deployment deployment = deploymentRepository.findByGithubRunId(runId)
                .orElseGet(() -> {
                    String cluster = project.getEcsClusterName() != null ? project.getEcsClusterName() : "";
                    String service = project.getEcsServiceName() != null ? project.getEcsServiceName() : "";

                    return deploymentRepository.save(
                            Deployment.builder()
                                    .project(project)
                                    .githubRunId(runId)
                                    .targetCluster(cluster)
                                    .targetService(service)
                                    .build());
                });

        DeploymentStage stage = deployment.getStages().stream()
                .filter(s -> s.getName() != null && s.getName().equals(jobName))
                .findFirst()
                .orElseGet(() -> {
                    DeploymentStage newStage = DeploymentStage.builder()
                            .name(jobName)
                            .githubJobId(jobId)
                            .status(ProgressStatus.PENDING)
                            .startedAt(startedAtLocal)
                            .build();
                    deployment.addStage(newStage);
                    return newStage;
                });

        if (conclusion != null) {
            if ("success".equalsIgnoreCase(conclusion)) {
                stage.complete();
            } else if ("failure".equalsIgnoreCase(conclusion) || "cancelled".equalsIgnoreCase(conclusion)) {
                stage.fail();
            } else {
                stage.setStatus(ProgressStatus.IN_PROGRESS);
            }
        } else {
            if ("in_progress".equalsIgnoreCase(status)) {
                stage.setStatus(ProgressStatus.IN_PROGRESS);
            } else if ("queued".equalsIgnoreCase(status)) {
                stage.setStatus(ProgressStatus.PENDING);
            }
        }

        if ("in_progress".equals(status) && stage.getStartedAt() == null) {
            stage.setStartedAt(LocalDateTime.now());
        }

        if ("completed".equals(status)) {
            stage.setCompletedAt(LocalDateTime.now());
        }

        if ("failure".equalsIgnoreCase(conclusion)) {
            deployment.setCiCheck(false);
        } else if ("success".equalsIgnoreCase(conclusion) && jobName.toLowerCase().contains("build")) {
            deployment.setCiCheck(true);
        }

        deploymentRepository.save(deployment);
        sseService.send(repoName, "deployment_update", stage);
    }

    @Transactional
    public void handleWorkflowRunWebhook(Map<String, Object> payload) {
        String action = (String) payload.get("action");

        if ("completed".equals(action)) {
            Map<String, Object> run = (Map<String, Object>) payload.get("workflow_run");
            Map<String, Object> repo = (Map<String, Object>) run.get("repository");
            Map<String, Object> owner = (Map<String, Object>) repo.get("owner");

            String runId = String.valueOf(run.get("id"));
            String conclusion = (String) run.get("conclusion");
            String repoName = (String) repo.get("name");
            String repoUrl = (String) repo.get("html_url");
            String ownerName = (String) owner.get("login");

            Project project = projectRepository.findByOwnerAndName(ownerName, repoName)
                    .orElseGet(() -> projectRepository.save(
                            Project.builder()
                                    .owner(ownerName)
                                    .name(repoName)
                                    .githubRepoUrl(repoUrl)
                                    .build()
                    ));

            Deployment deployment = deploymentRepository.findByGithubRunId(runId)
                    .orElseGet(() -> Deployment.builder()
                            .project(project)
                            .githubRunId(runId)
                            .targetCluster("")
                            .targetService("")
                            .build());

            Map<String, Object> headCommit = (Map<String, Object>) run.get("head_commit");
            String headBranch = (String) run.get("head_branch");

            if (headCommit != null) {
                String commitHash = (String) headCommit.get("id");
                deployment.setCommitHash(commitHash);
                deployment.setImageTag(commitHash.substring(0, 7));
                deployment.setCommitMessage((String) headCommit.get("message"));

                Map<String, Object> author = (Map<String, Object>) headCommit.get("author");
                if (author != null) {
                    deployment.setCommitAuthor((String) author.get("name"));
                }
            }
            deployment.setCommitBranch(headBranch);

            if ("success".equals(conclusion)) {
                deployment.setPipelineStatus(ProgressStatus.PENDING);
                deployment.setLastStep(Step.TEST);
                deployment.setCiCheck(true);
            } else {
                deployment.setCiCheck(false);
            }

            Deployment savedDeployment = deploymentRepository.save(deployment);
            sseService.send(savedDeployment.getProject().getName(), "deployment_complete", savedDeployment);
        }
    }

	/**
	 * Repository 내 모든 파이프라인 실행 리스트를 가져온다.
	 * 
	 * @param owner GitHub User / Organization
	 * @param repo  Repository Name
	 * @return Json 타입의 결과
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
	 * 
	 * @param owner GitHub User / Organization
	 * @param repo  Repository Name
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
				"ref", branchName);

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

    @Transactional
	public void streamLogsToEmitter(SseEmitter emitter, long deploymentId, long jobId) {
        Deployment deployment = deploymentRepository.findById(deploymentId).orElseThrow(() -> new IllegalArgumentException("Deployment not found"));

        Project project = deployment.getProject();

		executor.execute(() -> {
			try {
				URI rawLocation = getLogDownloadUrl(project.getOwner(), project.getName(), jobId);

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
	 * 
	 * @param owner GitHub User / Organization
	 * @param repo  Repository Name
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
