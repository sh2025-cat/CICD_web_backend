package cat.cicd.repository;

import cat.cicd.entity.WorkFlow;
import cat.cicd.entity.WorkflowJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowJob, Long> {
    Optional<WorkflowJob> findByWorkFlowAndName(WorkFlow workFlow, String name);
}
