package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_project_metrics")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProjectMetric {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false)
	private Double cpuUsage;

	@Column(nullable = false)
	private Double memoryUsage;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime recordedAt;

	public ProjectMetric(Project project, Double cpuUsage, Double memoryUsage) {
		this.project = project;
		this.cpuUsage = cpuUsage;
		this.memoryUsage = memoryUsage;
		this.recordedAt = LocalDateTime.now();
	}
}
