package cat.cicd.repository;

import cat.cicd.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
	Optional<Project> findById(long id);
    Optional<Project> findByOwnerAndName(String owner, String name);
}
