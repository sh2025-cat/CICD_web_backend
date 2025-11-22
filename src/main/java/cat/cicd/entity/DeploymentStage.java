package cat.cicd.entity;

import cat.cicd.global.enums.ProgressStatus;
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
	private ProgressStatus status;

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
		this.status = ProgressStatus.SUCCESS;
		this.completedAt = LocalDateTime.now();
	}

	public void fail() {
		this.status = ProgressStatus.FAILED;
		this.completedAt = LocalDateTime.now();
	}
}
