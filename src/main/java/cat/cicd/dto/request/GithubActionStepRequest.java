package cat.cicd.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class GithubActionStepRequest {
    private String repositoryName;
    private String githubRunId;
    private String jobName;
    private String stepName;
    private String status;
    private String log;
    private String startedAt;
    private String completedAt;
}
