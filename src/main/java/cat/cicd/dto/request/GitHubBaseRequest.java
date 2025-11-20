package cat.cicd.dto.request;

public record GitHubBaseRequest(
		String owner,
		String repo
) {

}
