package cat.cicd.repository;

import cat.cicd.entity.Project;
import cat.cicd.entity.ProjectMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<ProjectMetric, Long> {

    List<ProjectMetric> findTop5ByProjectOrderByRecordedAtDesc(Project project);

}
