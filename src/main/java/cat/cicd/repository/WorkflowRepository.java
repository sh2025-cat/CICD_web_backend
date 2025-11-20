package cat.cicd.repository;

import cat.cicd.entity.WorkFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkFlow, Long> {
    Optional<WorkFlow> findByGithubRunId(String githubRunId);
}
