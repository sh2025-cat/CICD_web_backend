package cat.cicd.dto.request;

import cat.cicd.entity.Project;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Builder
public record ProjectRequest (
		@NotBlank String owner,
		@NotBlank String name,
		@Nullable String githubRepoUrl
) {
    public Project toEntity() {
        return new Project(this.owner, this.name, this.githubRepoUrl);
    }
}
