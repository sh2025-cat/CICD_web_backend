package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_project")
@Getter
@Builder
@AllArgsConstructor
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

	public Project(String owner, String name, String githubRepoUrl) {
		this.owner = owner;
		this.name = name;
		this.githubRepoUrl = githubRepoUrl;
	}

}
