package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_deployment_stages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DeploymentStage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Setter
	private StageStatus status;

	private Long githubJobId;

	@CreationTimestamp
	@Setter
	private LocalDateTime startedAt;

	@Setter
	private LocalDateTime completedAt;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "deployment_id")
	private Deployment deployment;

	public void complete() {
		this.status = StageStatus.SUCCESS;
		this.completedAt = LocalDateTime.now();
	}

	public void fail() {
		this.status = StageStatus.FAILED;
		this.completedAt = LocalDateTime.now();
	}

	public enum StageStatus {
		PENDING, IN_PROGRESS, SUCCESS, FAILED, SKIPPED
	}
}
