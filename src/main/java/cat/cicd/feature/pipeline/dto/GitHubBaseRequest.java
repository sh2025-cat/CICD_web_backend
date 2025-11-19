package cat.cicd.feature.pipeline.dto;

public record GitHubBaseRequest(
		String owner,
		String repo
) {

}
