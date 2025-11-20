package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    @Setter
	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status;

    @Column(nullable = false)
    private String taskDefinitionArn;

    @Setter
	@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_deployment_id")
    private Deployment previousDeployment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DeploymentStatus {
        PENDING, IN_PROGRESS, SUCCESS, FAILED, ROLLED_BACK
    }

    public Deployment(Project project, Artifact artifact, DeploymentStatus status, String taskDefinitionArn) {
        this.project = project;
        this.artifact = artifact;
        this.status = status;
        this.taskDefinitionArn = taskDefinitionArn;
    }

}
