package cat.cicd.service;

import cat.cicd.entity.Artifact;
import cat.cicd.entity.Deployment;
import cat.cicd.entity.Project;
import cat.cicd.repository.ArtifactRepository;
import cat.cicd.repository.DeploymentRepository;
import cat.cicd.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ECSService {

    private final EcsClient ecsClient;
	private final ResourceGroupsTaggingApiClient taggingApiClient;
    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final ArtifactRepository artifactRepository;

    @Value("${aws.ecr.repository-uri-prefix}")
    private String ecrRepositoryUriPrefix;

	public ECSService(EcsClient ecsClient,
			ProjectRepository projectRepository, DeploymentRepository deploymentRepository,
			ArtifactRepository artifactRepository) {
		this.ecsClient = ecsClient;
		this.taggingApiClient = ResourceGroupsTaggingApiClient.create();
		this.projectRepository = projectRepository;
		this.deploymentRepository = deploymentRepository;
		this.artifactRepository = artifactRepository;
	}

	@Transactional
    public Deployment deployNewVersion(String serviceName, String clusterName, String containerName, String imageTag) {
        Project project = projectRepository.findByName(serviceName)
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceName));

        String imageUri = ecrRepositoryUriPrefix + "/" + project.getEcsServiceName() + ":" + imageTag;
        Artifact artifact = artifactRepository.findByImageUri(imageUri)
                .orElseGet(() -> artifactRepository.save(new Artifact(project.getEcsServiceName(), imageTag, imageUri)));

        DescribeServicesResponse describeServicesResponse = ecsClient.describeServices(
                b -> b.cluster(clusterName).services(serviceName));
        if (describeServicesResponse.services().isEmpty()) {
            throw new RuntimeException("ECS service not found: " + serviceName);
        }
        String currentTaskDefinitionArn = describeServicesResponse.services().getFirst().taskDefinition();
        TaskDefinition currentTaskDefinition = ecsClient.describeTaskDefinition(
                b -> b.taskDefinition(currentTaskDefinitionArn)).taskDefinition();

        List<ContainerDefinition> newContainerDefinitions = currentTaskDefinition.containerDefinitions().stream()
                .map(cd -> cd.name().equals(containerName) ? cd.toBuilder().image(imageUri).build() : cd)
                .collect(Collectors.toList());

        RegisterTaskDefinitionResponse registerTaskDefResponse = ecsClient.registerTaskDefinition(RegisterTaskDefinitionRequest.builder()
                .family(currentTaskDefinition.family())
                .volumes(currentTaskDefinition.volumes())
                .networkMode(currentTaskDefinition.networkMode())
                .requiresCompatibilities(currentTaskDefinition.requiresCompatibilities())
                .cpu(currentTaskDefinition.cpu())
                .memory(currentTaskDefinition.memory())
                .executionRoleArn(currentTaskDefinition.executionRoleArn())
                .taskRoleArn(currentTaskDefinition.taskRoleArn())
                .containerDefinitions(newContainerDefinitions)
                .build());

        String newTaskDefinitionArn = registerTaskDefResponse.taskDefinition().taskDefinitionArn();
        log.info("New Task Definition registered: {}", newTaskDefinitionArn);

        ecsClient.updateService(UpdateServiceRequest.builder()
                .cluster(clusterName)
                .service(serviceName)
                .taskDefinition(newTaskDefinitionArn)
                .forceNewDeployment(true)
                .build());
        log.info("Update service requested for: {}", serviceName);

        Optional<Deployment> previousSuccessfulDeployment = deploymentRepository.findFirstByProjectAndStatusOrderByIdDesc(
				project, Deployment.DeploymentStatus.SUCCESS);

        Deployment newDeployment = new Deployment(project, artifact, Deployment.DeploymentStatus.IN_PROGRESS, newTaskDefinitionArn);
        previousSuccessfulDeployment.ifPresent(newDeployment::setPreviousDeployment);

        return deploymentRepository.save(newDeployment);
    }

    @Transactional
    public Deployment rollbackToPreviousVersion(String serviceName, String clusterName) {
        Project project = projectRepository.findByName(serviceName)
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceName));

        Deployment lastSuccessfulDeployment = deploymentRepository.findFirstByProjectAndStatusOrderByIdDesc(project, Deployment.DeploymentStatus.SUCCESS)
                .orElseThrow(() -> new RuntimeException("No previous successful deployment found for service: " + serviceName));

        String rollbackTaskDefinitionArn = lastSuccessfulDeployment.getTaskDefinitionArn();
        log.info("Rolling back service {} to task definition {}", serviceName, rollbackTaskDefinitionArn);

        ecsClient.updateService(UpdateServiceRequest.builder()
                .cluster(clusterName)
                .service(serviceName)
                .taskDefinition(rollbackTaskDefinitionArn)
                .forceNewDeployment(true)
                .build());
        log.info("Rollback requested for service: {}", serviceName);

        Deployment rollbackDeployment = new Deployment(project, lastSuccessfulDeployment.getArtifact(), Deployment.DeploymentStatus.ROLLED_BACK, rollbackTaskDefinitionArn);
        rollbackDeployment.setPreviousDeployment(lastSuccessfulDeployment);

        return deploymentRepository.save(rollbackDeployment);
    }

	@Transactional
	public Project discoverAndSaveEcsInfo(String projectName) {
		log.info("Attempting to discover ECS info for project: {}", projectName);
		Project project = projectRepository.findByName(projectName)
				.orElseThrow(() -> new RuntimeException("Project not found with name: " + projectName));

		TagFilter tagFilter = TagFilter.builder()
				.key("ProjectRepo")
				.values(projectName)
				.build();

		GetResourcesRequest resourcesRequest = GetResourcesRequest.builder()
				.resourceTypeFilters("ecs:service")
				.tagFilters(tagFilter)
				.build();

		List<ResourceTagMapping> mappings = taggingApiClient.getResources(resourcesRequest).resourceTagMappingList();

		if (mappings.isEmpty()) {
			throw new RuntimeException("No tagged ECS service found for project: " + projectName);
		}
		if (mappings.size() > 1) {
			log.warn("Multiple tagged ECS services found for project: {}. Using the first one.", projectName);
		}

		String arn = mappings.getFirst().resourceARN();
		// arn:aws:ecs:ap-northeast-2:123456789012:service/dev-cluster/cat-frontend-svc
		String[] parts = arn.split(":");
		String[] serviceParts = parts[parts.length - 1].split("/");

		String clusterName = serviceParts[1];
		String serviceName = serviceParts[2];

		project.setEcsClusterName(clusterName);
		project.setEcsServiceName(serviceName);
		return projectRepository.save(project);
	}


	public List<String> listClusters() {
		return ecsClient.listClustersPaginator().stream()
				.flatMap(response -> response.clusterArns().stream())
				.collect(Collectors.toList());
	}


	public List<String> listServices(long projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));

		if(project.getEcsClusterName().isEmpty()) {
			throw new IllegalArgumentException("Ecs Cluster name not found, Please synchronization first");
		}

		ListServicesRequest request = ListServicesRequest.builder().cluster(project.getEcsClusterName()).build();
		return ecsClient.listServicesPaginator(request).stream()
				.flatMap(response -> response.serviceArns().stream())
				.collect(Collectors.toList());
	}
}
