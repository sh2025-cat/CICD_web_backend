package cat.cicd.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentStep {
	private String name;
	private Long githubJobId;
	private String status;
	private String eventLogs;
	private LocalDateTime startedAt;
	private LocalDateTime completedAt;
}