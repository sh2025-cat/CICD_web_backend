package cat.cicd.dto.response;

import cat.cicd.entity.Project;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectResponse {
    private final Long id;
    private final String owner;
    private final String name;
    private final String githubRepoUrl;
    private final String ecsCluster;
    private final String ecsService;

    public static ProjectResponse from(Project project) {
        if (project == null) return null;
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
