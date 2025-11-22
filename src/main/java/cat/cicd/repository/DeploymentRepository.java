package cat.cicd.repository;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.Project;
import cat.cicd.global.enums.ProgressStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
	Optional<Deployment> findFirstByProjectAndPipelineStatusEqualsOrderByIdDesc(Project project, ProgressStatus progressStatus);

	Optional<Deployment> findByGithubRunId(String githubRunId);

	Deployment findTopByProjectOrderByCreatedAtDesc(Project project);

	List<Deployment> findAllByProjectIdAndCiCheckEqualsOrderByCreatedAtDesc(Long projectId, boolean ciCheck);

    List<Deployment> findByProjectAndPipelineStatusEqualsOrderByCreatedAtDesc(Project project, ProgressStatus progressStatus, Pageable pageable);

    List<Deployment> findAllByPipelineStatus(ProgressStatus status);
}
