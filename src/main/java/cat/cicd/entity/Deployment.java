package cat.cicd.entity;

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
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deployment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(unique = true)
	private String githubRunId;

	@Setter
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "previous_deployment_id")
	private Deployment previousDeployment;

	@Setter
	@Column
	private String commitHash;
	@Setter
	@Column
	private String commitMessage;
	@Setter
	@Column
	private String commitAuthor;
	@Setter
	@Column
	private String commitBranch;
	@Setter
	@Column
	private String imageTag;
    @Setter
    @Column
    private String lastStep;
    @Setter
    @Column
    private String pipelineStatus;

    @Setter
	@Column(nullable = false)
	private String targetCluster;

    @Setter
	@Column(nullable = false)
	private String targetService;

	@Setter
	@Column
	private String taskDefinitionArn;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DeploymentStatus status;

	@OneToMany(mappedBy = "deployment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DeploymentStage> stages = new ArrayList<>();

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	@Builder
	public Deployment(Project project, String githubRunId, String targetCluster, String targetService) {
		this.project = project;
		this.githubRunId = githubRunId;
		this.targetCluster = targetCluster;
		this.targetService = targetService;
		this.status = DeploymentStatus.PENDING;
	}

	public void addStage(DeploymentStage stage) {
		this.stages.add(stage);
		stage.setDeployment(this);
	}

	public enum DeploymentStatus {
		PENDING, IN_PROGRESS, SUCCESS, FAILED
	}
}