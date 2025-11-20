package cat.cicd.repository;

import cat.cicd.entity.ProjectMetric;
import cat.cicd.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<ProjectMetric, Long> {
    List<ProjectMetric> findByProjectAndRecordedAtAfter(Project project, LocalDateTime startTime);
}
