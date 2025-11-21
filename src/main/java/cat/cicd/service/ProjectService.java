package cat.cicd.service;

import cat.cicd.dto.request.ProjectRequest;
import cat.cicd.dto.response.DeploymentHistoryResponse;
import cat.cicd.dto.response.RepoDeployStatusResponse;
import cat.cicd.entity.Deployment;
import cat.cicd.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final cat.cicd.repository.DeploymentRepository deploymentRepository;

	public ProjectService(ProjectRepository projectRepository,
			cat.cicd.repository.DeploymentRepository deploymentRepository) {
		this.projectRepository = projectRepository;
		this.deploymentRepository = deploymentRepository;
	}

	public void createProject(ProjectRequest projectRequest) {
		projectRepository.save(projectRequest.toEntity());
	}

	public java.util.List<RepoDeployStatusResponse> getRepoDeployStatuses() {
		return projectRepository.findAll().stream()
				.map(project -> {
					Deployment latestDeployment = deploymentRepository
							.findTopByProjectOrderByCreatedAtDesc(project);
					if (latestDeployment != null) {
						return RepoDeployStatusResponse.of(
								project.getId(),
								project.getName(),
                                latestDeployment.getStatus().name(),
								latestDeployment.getImageTag(),
								latestDeployment.getCommitHash(),
								latestDeployment.getCommitMessage(),
								latestDeployment.getCreatedAt());
					} else {
						return RepoDeployStatusResponse.of(
								project.getId(),
								project.getName(),
                                null, Deployment.DeploymentStatus.PENDING.name(), null, null, null);
					}
				})
				.toList();
	}

	public java.util.List<DeploymentHistoryResponse> getDeploymentHistory(Long projectId) {
		return deploymentRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
				.map(DeploymentHistoryResponse::from)
				.toList();
	}

	public cat.cicd.dto.response.DeploymentDetailResponse getDeploymentDetail(Long deploymentId) {
		Deployment deployment = deploymentRepository.findById(deploymentId)
				.orElseThrow(() -> new IllegalArgumentException("Deployment not found with id: " + deploymentId));
		return cat.cicd.dto.response.DeploymentDetailResponse.from(deployment);
	}
}
