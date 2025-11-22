package cat.cicd.service;

import cat.cicd.dto.request.NextStepRequest;
import cat.cicd.dto.request.ProjectRequest;
import cat.cicd.dto.response.DeploymentHistoryResponse;
import cat.cicd.dto.response.RepoDeployStatusResponse;
import cat.cicd.entity.Deployment;
import cat.cicd.global.enums.ProgressStatus;
import cat.cicd.global.enums.Step;
import cat.cicd.repository.DeploymentRepository;
import cat.cicd.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final DeploymentRepository deploymentRepository;

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
								latestDeployment.getImageTag(),
                                latestDeployment.getPipelineStatus(),
								latestDeployment.getCommitHash() != null
                                        ? latestDeployment.getCommitHash().substring(0, 7)
                                        : null,
								latestDeployment.getCommitMessage(),
								latestDeployment.getCreatedAt());
					} else {
						return RepoDeployStatusResponse.of(
								project.getId(),
								project.getName(),
                                null,
                                ProgressStatus.PENDING,
                                null,
                                null,
                                null);
					}
				})
				.toList();
	}

    @Transactional
	public java.util.List<DeploymentHistoryResponse> getDeploymentHistory(Long projectId) {
		return deploymentRepository.findAllByProjectIdAndCiCheckEqualsOrderByCreatedAtDesc(projectId, true).stream()
				.map(DeploymentHistoryResponse::from)
				.toList();
	}

    @Transactional
	public cat.cicd.dto.response.DeploymentDetailResponse getDeploymentDetail(Long deploymentId) {
		Deployment deployment = deploymentRepository.findById(deploymentId)
				.orElseThrow(() -> new IllegalArgumentException("Deployment not found with id: " + deploymentId));

        deployment.setPipelineStatus(ProgressStatus.IN_PROGRESS);
		return cat.cicd.dto.response.DeploymentDetailResponse.from(deployment);
	}

    public void postNextStep(long deploymentId, NextStepRequest request) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found with id: " + deploymentId));

        deployment.setLastStep(Step.valueOf(request.step()));
        deploymentRepository.save(deployment);
    }
}
