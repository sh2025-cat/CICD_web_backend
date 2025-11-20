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
@Table(name = "tb_workflow_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private WorkFlow workFlow;

    @Column(nullable = false)
    private String name;

    @Setter
	@Column(nullable = false)
    private String status;

    @Setter
	@Lob
    @Column(columnDefinition = "TEXT")
    private String log;

    @Setter
	private LocalDateTime startedAt;

    @Setter
	private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WorkflowJob(WorkFlow workFlow, String name, String status) {
        this.workFlow = workFlow;
        this.name = name;
        this.status = status;
    }
}
