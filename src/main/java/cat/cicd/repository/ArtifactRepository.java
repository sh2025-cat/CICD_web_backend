package cat.cicd.repository;

import cat.cicd.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    Optional<Artifact> findByImageUri(String imageUri);
    List<Artifact> findByRepositoryNameOrderByCreatedAtDesc(String repositoryName);
}
