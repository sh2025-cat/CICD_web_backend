package cat.cicd.repository;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    Optional<Deployment> findFirstByProjectAndStatusOrderByIdDesc(Project project, Deployment.DeploymentStatus status);
    List<Deployment> findByProjectOrderByIdDesc(Project project);
}
