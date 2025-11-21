package cat.cicd.service;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.DeploymentStage;
import cat.cicd.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.DescribeServicesResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final EcsClient ecsClient;
    private final DeploymentRepository deploymentRepository;

    @Transactional
    public void checkDeploymentStatus(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));

        if (deployment.getStatus() != Deployment.DeploymentStatus.IN_PROGRESS) {
            log.info("Deployment {} is not in progress. Status: {}", deploymentId, deployment.getStatus());
            return;
        }

        String clusterName = deployment.getTargetCluster();
        String serviceName = deployment.getTargetService();

        DescribeServicesResponse response = ecsClient
                .describeServices(b -> b.cluster(clusterName).services(serviceName));

        if (response.services().isEmpty()) {
            log.error("Service not found: {}/{}", clusterName, serviceName);
            failDeployment(deployment, "Service not found");
            return;
        }

        software.amazon.awssdk.services.ecs.model.Service service = response.services().getFirst();

        // Simple check: if running count matches desired count and deployments size is
        // 1 (steady state)
        // This is a basic check. Real-world monitoring might be more complex.
        boolean isStable = service.runningCount() == service.desiredCount() && service.deployments().size() == 1;

        if (isStable) {
            log.info("Deployment {} is stable.", deploymentId);
            completeDeployment(deployment);
        } else {
            log.info("Deployment {} is still in progress. Running: {}, Desired: {}, Deployments: {}",
                    deploymentId, service.runningCount(), service.desiredCount(), service.deployments().size());
        }
    }

    private void completeDeployment(Deployment deployment) {
        deployment.setStatus(Deployment.DeploymentStatus.SUCCESS);
        // Find the "Deployment" stage and complete it
        deployment.getStages().stream()
                .filter(stage -> "deploy".equalsIgnoreCase(stage.getName())
                        && stage.getStatus() == DeploymentStage.StageStatus.IN_PROGRESS)
                .findFirst()
                .ifPresent(DeploymentStage::complete);
        deploymentRepository.save(deployment);
    }

    private void failDeployment(Deployment deployment, String reason) {
        deployment.setStatus(Deployment.DeploymentStatus.FAILED);
        deployment.getStages().stream()
                .filter(stage -> "deploy".equalsIgnoreCase(stage.getName())
                        && stage.getStatus() == DeploymentStage.StageStatus.IN_PROGRESS)
                .findFirst()
                .ifPresent(DeploymentStage::fail);
        deploymentRepository.save(deployment);
    }
}
