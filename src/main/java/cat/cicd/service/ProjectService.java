package cat.cicd.service;

import cat.cicd.dto.request.ProjectRequest;
import cat.cicd.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;

	public ProjectService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	public void createProject(ProjectRequest projectRequest) {
		projectRepository.save(projectRequest.toEntity());
	}
}
