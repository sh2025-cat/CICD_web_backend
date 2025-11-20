package cat.cicd.service;

import cat.cicd.entity.ProjectMetric;
import cat.cicd.entity.Project;
import cat.cicd.repository.MetricRepository;
import cat.cicd.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class MonitoringService {

    private final CloudWatchClient cloudWatchClient;
    private final ProjectRepository projectRepository;
    private final MetricRepository metricRepository;

	public MonitoringService(ProjectRepository projectRepository,
			MetricRepository metricRepository) {
		this.cloudWatchClient = CloudWatchClient.create();
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
}
