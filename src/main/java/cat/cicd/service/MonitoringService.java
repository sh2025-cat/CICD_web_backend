package cat.cicd.service;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.DeploymentStage;
import cat.cicd.entity.Project;
import cat.cicd.entity.ProjectMetric;
import cat.cicd.global.enums.ProgressStatus;
import cat.cicd.repository.DeploymentRepository;
import cat.cicd.repository.MetricRepository;
import cat.cicd.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.DescribeServicesResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MonitoringService {

    private final EcsClient ecsClient;
    private final DeploymentRepository deploymentRepository;
    private final CloudWatchClient cloudWatchClient;
    private final ProjectRepository projectRepository;
    private final MetricRepository metricRepository;
    private final SseService sseService;

    public MonitoringService(DeploymentRepository deploymentRepository, ProjectRepository projectRepository, MetricRepository metricRepository, SseService sseService) {
        this.sseService = sseService;
        this.ecsClient = EcsClient.create();
        this.cloudWatchClient = CloudWatchClient.create();
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
        this.metricRepository = metricRepository;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void collectAndStoreMetrics() {
        log.info("Starting scheduled metrics collection...");
        List<Project> repositories = projectRepository.findAll();
        for (Project project : repositories) {
            try {
                collectMetricsForService(project);
            } catch (Exception e) {
                log.error("Failed to collect metrics for service: {}", project.getName(), e);
            }
        }
        log.info("Finished scheduled metrics collection.");
    }

    private void collectMetricsForService(Project project) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(5, ChronoUnit.MINUTES);


        MetricDataQuery cpuQuery = createMetricQuery("cpu_utilization", project.getEcsServiceName(), project.getEcsClusterName(), "CPUUtilization");
        MetricDataQuery memQuery = createMetricQuery("memory_utilization", project.getEcsServiceName(), project.getEcsClusterName(), "MemoryUtilization");

        GetMetricDataRequest request = GetMetricDataRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .metricDataQueries(cpuQuery, memQuery)
                .scanBy(ScanBy.TIMESTAMP_DESCENDING)
                .build();

        GetMetricDataResponse response = cloudWatchClient.getMetricData(request);

        Double latestCpu = 0.0;
        Double latestMem = 0.0;

        for (MetricDataResult result : response.metricDataResults()) {
            if (!result.values().isEmpty()) {
                if (result.id().equals("cpu_utilization")) {
                    latestCpu = result.values().getFirst();
                } else if (result.id().equals("memory_utilization")) {
                    latestMem = result.values().getFirst();
                }
            }
        }

        if (latestCpu > 0 || latestMem > 0) {
            ProjectMetric projectMetric = new ProjectMetric(project, latestCpu, latestMem);
            metricRepository.save(projectMetric);
            log.info("Saved new metric for service {}: CPU={}, Mem={}", project.getName(), latestCpu, latestMem);
        } else {
            log.info("No new metric data found for service {}", project.getName());

        }
    }

    private MetricDataQuery createMetricQuery(String id, String serviceName, String clusterName, String metricName) {
        return MetricDataQuery.builder()
                .id(id)
                .metricStat(MetricStat.builder()
                        .stat("Average")
                        .period(60)
                        .metric(Metric.builder()
                                .namespace("AWS/ECS")
                                .metricName(metricName)
                                .dimensions(
                                        Dimension.builder().name("ClusterName").value(clusterName).build(),
                                        Dimension.builder().name("ServiceName").value(serviceName).build()
                                )
                                .build())
                        .build())
                .returnData(true)
                .build();


    }

    @Transactional
    public void checkDeploymentStatus(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found"));

        if (deployment.getDeployStatus() != ProgressStatus.IN_PROGRESS) return;

//        if (deployment.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(20))) {
//            log.error("Deployment {} timed out.", deploymentId);
//            failDeployment(deployment, "배포 시간 초과 (Time out)");
//            return;
//        }
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

        boolean isStable = Objects.equals(service.runningCount(), service.desiredCount())
                && service.deployments().size() == 1
                && "PRIMARY".equals(service.deployments().getFirst().status());

        if (isStable) {
            log.info("Deployment {} successfully completed.", deploymentId);
            completeDeployment(deployment);
        } else {
            log.debug("Deployment {} in progress... Running: {}/{}",
                    deploymentId, service.runningCount(), service.desiredCount());
        }
    }

    private void completeDeployment(Deployment deployment) {
        deployment.setDeployStatus(ProgressStatus.SUCCESS);
        deployment.getStages().stream()
                .filter(stage -> "deploy".equalsIgnoreCase(stage.getName())
                        && stage.getStatus() == ProgressStatus.IN_PROGRESS)
                .findFirst()
                .ifPresent(DeploymentStage::complete);
        deploymentRepository.save(deployment);

        sseService.send(deployment.getProject().getId(), "deployment_complete", ProgressStatus.SUCCESS);
    }

    private void failDeployment(Deployment deployment, String reason) {
        deployment.setDeployStatus(ProgressStatus.FAILED);
        deployment.getStages().stream()
                .filter(stage -> "deploy".equalsIgnoreCase(stage.getName())
                        && stage.getStatus() == ProgressStatus.IN_PROGRESS)
                .findFirst()
                .ifPresent(DeploymentStage::fail);
        deploymentRepository.save(deployment);

        sseService.send(deployment.getProject().getId(), "deployment_complete", ProgressStatus.FAILED);
    }
}
