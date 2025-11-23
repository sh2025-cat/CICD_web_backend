package cat.cicd.entity;

import cat.cicd.global.enums.ProgressStatus;
import cat.cicd.global.enums.Step;
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

	@Column
	private String githubRunId;

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
    private Step lastStep;
    @Setter
    @Column
    @Builder.Default
    private ProgressStatus pipelineStatus = ProgressStatus.PENDING;
    @Setter
    @Column
    @Builder.Default
    private ProgressStatus deployStatus = ProgressStatus.PENDING;

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
    @Column
    private String beforeTaskDefinitionArn;

    @Setter
    @Column
    private boolean ciCheck;

    @Builder.Default
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
		this.ciCheck = false;
        this.pipelineStatus = ProgressStatus.PENDING;
	}

	public void addStage(DeploymentStage stage) {
        if(this.stages == null) this.stages = new ArrayList<>();
		this.stages.add(stage);
		stage.setDeployment(this);
	}
}