package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "artifact_id")
	private Artifact artifact;

	@Column(nullable = false, unique = true)
	private Long runId;

	@Setter
	@Column(name = "task_definition_arn")
	private String taskDefinitionArn;

	@Column(name = "target_cluster")
	private String targetCluster;

	@Column(name = "target_service")
	private String targetService;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DeploymentStatus status;

	@OneToMany(mappedBy = "deployment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DeploymentLog> logs = new ArrayList<>();

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	public enum DeploymentStatus {
		PENDING, IN_PROGRESS, SUCCESS, FAILED, CANCELLED
	}

	public Deployment(Project project, Long runId) {
		this.project = project;
		this.runId = runId;
		// ★ 여기가 핵심: 생성 시점의 Project 설정을 스냅샷으로 저장
		this.targetCluster = project.getEcsClusterName();
		this.targetService = project.getEcsServiceName();
		this.status = DeploymentStatus.PENDING;
	}
}