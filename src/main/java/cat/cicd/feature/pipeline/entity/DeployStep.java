package cat.cicd.feature.pipeline.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class DeployStep {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;			// Step 이름
	private int stepOrder;			// 순서
	private String status;			// success, failure
	private LocalDateTime startAt;	// Step 시작 시간
	private LocalDateTime endedAt;	// Step 종료 시간

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String logContent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id")
	private DeployJob deployJob;
}
