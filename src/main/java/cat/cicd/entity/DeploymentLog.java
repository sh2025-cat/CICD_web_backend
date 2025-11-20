package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_deployment_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

	@Column(nullable = false)
	private Long jobId;

    @Column(nullable = false)
    private String stepName;

    @Setter
    @Column(nullable = false)
    private String status;

    @Setter
    @Lob
    private String logContent;

    @Setter
    private LocalDateTime startedAt;

    @Setter
    private LocalDateTime completedAt;

    public DeploymentLog(Deployment deployment, long jobId, String stepName, String status) {
        this.deployment = deployment;
        this.stepName = stepName;
        this.status = status;
		this.jobId = jobId;
    }
}