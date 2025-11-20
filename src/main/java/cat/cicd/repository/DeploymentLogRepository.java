package cat.cicd.repository;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.DeploymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, Long> {
	Optional<DeploymentLog> findByDeploymentAndStepName(Deployment deployment, String stepName);
}
