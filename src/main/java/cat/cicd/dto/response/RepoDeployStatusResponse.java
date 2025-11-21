package cat.cicd.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RepoDeployStatusResponse {
	private Long id;
	private String name;
	private String tag;
	private String commitHash;
	private String commitMsg;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	private LocalDateTime deployedAt;

	public static RepoDeployStatusResponse of(Long id, String name, String tag, String commitHash, String commitMsg, LocalDateTime deployedAt) {
		return RepoDeployStatusResponse.builder()
				.id(id)
				.name(name)
				.tag(tag)
				.commitHash(commitHash)
				.commitMsg(commitMsg)
				.deployedAt(deployedAt)
				.build();
	}
}
