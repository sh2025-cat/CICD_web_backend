package cat.cicd.dto.response;

import cat.cicd.entity.Artifact;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ArtifactResponse {
    private final Long id;
    private final String repositoryName;
    private final String imageTag;
    private final String imageUri;
    private final String commitHash;
    private final LocalDateTime createdAt;

    public static ArtifactResponse from(Artifact artifact) {
        if (artifact == null) return null;
        return ArtifactResponse.builder()
                .id(artifact.getId())
                .repositoryName(artifact.getRepositoryName())
                .imageTag(artifact.getImageTag())
                .imageUri(artifact.getImageUri())
                .commitHash(artifact.getCommitHash())
                .createdAt(artifact.getCreatedAt())
                .build();
    }
}
