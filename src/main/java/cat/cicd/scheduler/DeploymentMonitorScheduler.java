package cat.cicd.scheduler;

import cat.cicd.entity.Deployment;
import cat.cicd.global.enums.ProgressStatus;
import cat.cicd.repository.DeploymentRepository;
import cat.cicd.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeploymentMonitorScheduler {

    private final DeploymentRepository deploymentRepository;
    private final MonitoringService monitoringService;

    @Scheduled(fixedDelay = 10000)
    public void pollDeploymentStatus() {
        List<Deployment> ongoingDeployments = deploymentRepository.findAllByDeployStatus(ProgressStatus.IN_PROGRESS);

        for (Deployment deployment : ongoingDeployments) {
            try {
                monitoringService.checkDeploymentStatus(deployment.getId());
            } catch (Exception e) {
                log.error("배포 상태 확인 중 에러 발생: {}", deployment.getId(), e);
            }
        }
    }
}