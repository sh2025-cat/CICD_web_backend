package cat.cicd.repository;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.Project;
import cat.cicd.global.enums.ProgressStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    @EntityGraph(attributePaths = {"stages"})
	Deployment findFirstByProjectAndPipelineStatusEqualsOrderByIdDesc(Project project, ProgressStatus progressStatus);

    @EntityGraph(attributePaths = {"stages"})
	Optional<Deployment> findByGithubRunId(String githubRunId);

    @EntityGraph(attributePaths = {"stages"})
	Deployment findTopByProjectOrderByCreatedAtDesc(Project project);

    @EntityGraph(attributePaths = {"stages"})
	List<Deployment> findAllByProjectIdAndCiCheckEqualsOrderByCreatedAtDesc(Long projectId, boolean ciCheck);

    List<Deployment> findByProjectAndPipelineStatusEqualsOrderByCreatedAtDesc(Project project, ProgressStatus progressStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"stages"})
    List<Deployment> findAllByPipelineStatus(ProgressStatus status);

    List<Deployment> findAllByDeployStatus(ProgressStatus deployStatus);
}
