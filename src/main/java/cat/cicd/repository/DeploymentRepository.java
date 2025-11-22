package cat.cicd.repository;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.Project;
import cat.cicd.global.enums.DeploymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
	Optional<Deployment> findFirstByProjectAndStatusOrderByIdDesc(Project project, DeploymentStatus status);

	Optional<Deployment> findByGithubRunId(String githubRunId);

	Deployment findTopByProjectOrderByCreatedAtDesc(Project project);

	List<Deployment> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Deployment> findByProjectAndStatusOrderByCreatedAtDesc(Project project, DeploymentStatus status, Pageable pageable);

    List<Deployment> findAllByStatus(DeploymentStatus status);
}
