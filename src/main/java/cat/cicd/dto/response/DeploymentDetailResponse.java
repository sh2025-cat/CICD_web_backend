package cat.cicd.dto.response;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.vo.DeploymentStep;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class DeploymentDetailResponse {

    private Long id;
    private String status;
    private String githubRunId;
    private String githubRunUrl;
    
    private TimeInfo timings;
    private MetaInfo meta;
    private CommitInfo commit;
    private List<StepInfo> steps;

    @Getter @Builder
    public static class TimeInfo {
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;
        private String duration;
    }

    @Getter @Builder
    public static class MetaInfo {
        private String project;
        private String cluster;
        private String service;
        private String taskDefArn;
        private String imageTag;
    }

    @Getter @Builder
    public static class CommitInfo {
        private String message;
        private String hash;
        private String author;
        private String branch;
    }

    @Getter @Builder
    public static class StepInfo {
        private String name;
        private String status;
        private String duration;
        private Long githubJobId;
        private LocalDateTime startedAt;
    }

    public static DeploymentDetailResponse from(Deployment deployment) {
        return DeploymentDetailResponse.builder()
                .id(deployment.getId())
                .status(deployment.getStatus().name())
                .githubRunId(deployment.getGithubRunId())
                .githubRunUrl("https://github.com/YOUR_ORG/YOUR_REPO/actions/runs/" + deployment.getGithubRunId())
                
                .timings(TimeInfo.builder()
                        .createdAt(deployment.getCreatedAt())
                        .finishedAt(deployment.getUpdatedAt())
                        .duration(formatDuration(deployment.getCreatedAt(), deployment.getUpdatedAt()))
                        .build())
                
                .meta(MetaInfo.builder()
                        .project(deployment.getProject().getName())
                        .cluster(deployment.getTargetCluster())
                        .service(deployment.getTargetService())
                        .taskDefArn(deployment.getTaskDefinitionArn())
                        .imageTag(deployment.getImageTag())
                        .build())
                
                .commit(CommitInfo.builder()
                        .message(deployment.getCommitMessage())
                        .hash(deployment.getCommitHash())
                        .author(deployment.getCommitAuthor())
                        .branch(deployment.getCommitBranch())
                        .build())
                
                .steps(deployment.getSteps().stream()
                        .map(DeploymentDetailResponse::mapStep)
                        .collect(Collectors.toList()))
                .build();
    }

    private static StepInfo mapStep(DeploymentStep step) {
        return StepInfo.builder()
                .name(step.getName())
                .status(step.getStatus())
                .githubJobId(step.getGithubJobId())
                .startedAt(step.getStartedAt())
                .duration(formatDuration(step.getStartedAt(), step.getCompletedAt()))
                .build();
    }

    private static String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "-";
        long seconds = Duration.between(start, end).getSeconds();
        if (seconds < 60) return seconds + "s";
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }
}