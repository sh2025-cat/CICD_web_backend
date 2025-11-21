package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String owner;

	@Column(nullable = false, unique = true)
	private String name;

	@Column
	private String githubRepoUrl;

	@Setter
	@Column
	private String ecsClusterName;

	@Setter
	@Column
	private String ecsServiceName;

	@Setter
	@Column
	private String containerName;

	public Project(String owner, String name, String githubRepoUrl) {
		this.owner = owner;
		this.name = name;
		this.githubRepoUrl = githubRepoUrl;
	}

}
