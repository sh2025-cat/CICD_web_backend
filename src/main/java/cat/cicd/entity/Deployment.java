package cat.cicd.entity;

import cat.cicd.entity.vo.DeploymentStep;
import cat.cicd.global.converter.DeploymentStepConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_deployments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deployment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, unique = true)
	private String githubRunId;

	@Setter @Column private String commitHash;
	@Setter @Column private String commitMessage;
	@Setter @Column private String commitAuthor;
	@Setter @Column private String commitBranch;
	@Setter @Column private String imageTag;

	@Column(nullable = false)
	private String targetCluster;

	@Column(nullable = false)
	private String targetService;

	@Setter @Column
	private String taskDefinitionArn;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DeploymentStatus status;

	@Setter
	@Convert(converter = DeploymentStepConverter.class)
	@Column(columnDefinition = "TEXT")
	private List<DeploymentStep> steps = new ArrayList<>();

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	public enum DeploymentStatus {
		PENDING, IN_PROGRESS, SUCCESS, FAILED, CANCELLED, ROLLED_BACK
	}

	@Builder
	public Deployment(Project project, String githubRunId, String targetCluster, String targetService) {
		this.project = project;
		this.githubRunId = githubRunId;
		this.targetCluster = targetCluster;
		this.targetService = targetService;
		this.status = DeploymentStatus.PENDING;
	}

	public void updateStep(DeploymentStep newStep) {
		this.steps.removeIf(step -> step.getName().equals(newStep.getName()));
		this.steps.add(newStep);
	}
}