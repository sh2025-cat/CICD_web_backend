package cat.cicd.dto.response;

public record MetricResponse(
        Double cpuUsage,
        Double memoryUsage,
        String recordTime
) {
}
