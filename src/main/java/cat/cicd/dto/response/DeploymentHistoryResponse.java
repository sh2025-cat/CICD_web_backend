package cat.cicd.dto.response;

import cat.cicd.entity.Deployment;
import cat.cicd.global.enums.ProgressStatus;
import cat.cicd.global.enums.Step;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DeploymentHistoryResponse {
	private Long deploymentId;
    private Step lastStep;
    private ProgressStatus pipelineStatus;
	private CommitInfo commit;
	private Timings timings;
	private List<StageInfo> stages;

	public static DeploymentHistoryResponse from(Deployment deployment) {
		CommitInfo commitInfo = CommitInfo.builder()
                .message(deployment.getCommitMessage())
                .shortHash(deployment.getCommitHash() != null && deployment.getCommitHash().length() > 7
								? deployment.getCommitHash().substring(0, 7)
								: deployment.getCommitHash())
                .branch(deployment.getCommitBranch())
				.authorName(deployment.getCommitAuthor())
                .build();

		LocalDateTime startedAt = deployment.getCreatedAt();
		LocalDateTime completedAt = deployment.getUpdatedAt();
		String durationStr = "";
		if (startedAt != null && completedAt != null) {
			Duration duration = Duration.between(startedAt, completedAt);
			durationStr = duration.toSeconds() + "s";
		}

		Timings timings = Timings.builder()
				.startedAt(startedAt)
				.completedAt(completedAt)
				.duration(durationStr)
				.build();

		List<StageInfo> stages = deployment.getStages().stream()
				.map(step ->
						StageInfo.builder()
								.name(step.getName())
								.status(String.valueOf(step.getStatus()))
								.build()
				).toList();

		return DeploymentHistoryResponse.builder()
                .deploymentId(deployment.getId())
                .lastStep(deployment.getLastStep())
                .pipelineStatus(deployment.getPipelineStatus())
                .commit(commitInfo)
                .timings(timings)
                .stages(stages)
                .build();
	}

	@Getter
	@Builder
	public static class CommitInfo {
		private String message;
		private String shortHash;
		private String branch;
		private String authorName;
	}

	@Getter
	@Builder
	public static class Timings {
		private LocalDateTime startedAt;
		private LocalDateTime completedAt;
		private String duration;
	}

	@Getter
	@Builder
	public static class StageInfo {
		private String name;
		private String status;
	}
}
