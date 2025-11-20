package cat.cicd.dto.response;

import cat.cicd.entity.Artifact;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ArtifactResponse(
		Long id,
		String repositoryName,
		String imageTag,
		String imageUri,
		String commitHash,
		LocalDateTime createdAt
) {
	public static ArtifactResponse from(Artifact artifact) {
		if (artifact == null)
			return null;
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
