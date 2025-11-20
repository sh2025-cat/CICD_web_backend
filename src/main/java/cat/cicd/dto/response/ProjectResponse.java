package cat.cicd.dto.response;

import cat.cicd.entity.Project;
import lombok.Builder;

@Builder
public record ProjectResponse(
		Long id,
		String owner,
		String name,
		String githubRepoUrl,
		String ecsCluster,
		String ecsService
) {
	public static ProjectResponse from(Project project) {
		if (project == null)
			return null;
		return ProjectResponse.builder()
				.id(project.getId())
				.owner(project.getOwner())
				.name(project.getName())
				.githubRepoUrl(project.getGithubRepoUrl())
				.ecsCluster(project.getEcsClusterName())
				.ecsService(project.getEcsServiceName())
				.build();
	}
}
