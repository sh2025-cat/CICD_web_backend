package cat.cicd.dto.response;

import cat.cicd.entity.Project;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceResponse {
    private final Long id;
    private final String name;
    private final String githubRepoUrl;

    public static ServiceResponse from(Project project) {
        if (project == null) return null;
        return ServiceResponse.builder()
                .id(project.getId())
                .name(project.getEcsServiceName())
                .githubRepoUrl(project.getGithubRepoUrl())
                .build();
    }
}
