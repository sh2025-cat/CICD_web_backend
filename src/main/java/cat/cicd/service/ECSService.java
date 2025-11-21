package cat.cicd.service;

import cat.cicd.entity.Deployment;
import cat.cicd.entity.Project;

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

        @Value("${aws.ecr.repository-uri-prefix}")
        private String ecrRepositoryUriPrefix;

        public ECSService(EcsClient ecsClient, ProjectRepository projectRepository, DeploymentRepository deploymentRepository) {
			this.ecsClient = ecsClient;
			this.taggingApiClient = ResourceGroupsTaggingApiClient.create();
            this.projectRepository = projectRepository;
            this.deploymentRepository = deploymentRepository;
        }

        @Transactional
        public Deployment deployNewVersion(Long deploymentId, String imageTag) {
                Deployment deployment = deploymentRepository.findById(deploymentId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Deployment not found with id: " + deploymentId));

                Project project = deployment.getProject();

                if (project.getEcsClusterName() == null || project.getEcsClusterName().isEmpty()) {
                        throw new IllegalArgumentException(
                                        "ECS Cluster name not found for project: " + project.getName());
                }
                if (project.getEcsServiceName() == null || project.getEcsServiceName().isEmpty()) {
                        throw new IllegalArgumentException(
                                        "ECS Service name not found for project: " + project.getName());
                }

                return deployNewVersion(deployment, project, imageTag);
        }

        @Transactional
        protected Deployment deployNewVersion(Deployment deployment, Project project, String imageTag) {
                String imageUri = ecrRepositoryUriPrefix + "/" + project.getEcsServiceName() + ":" + imageTag;

                DescribeServicesResponse describeServicesResponse = ecsClient.describeServices(
                                b -> b.cluster(project.getEcsClusterName()).services(project.getEcsServiceName()));
                if (describeServicesResponse.services().isEmpty()) {
                        throw new RuntimeException("ECS service not found: " + project.getEcsServiceName());
                }
                String currentTaskDefinitionArn = describeServicesResponse.services().getFirst().taskDefinition();
                TaskDefinition currentTaskDefinition = ecsClient.describeTaskDefinition(
                                b -> b.taskDefinition(currentTaskDefinitionArn)).taskDefinition();

                List<ContainerDefinition> newContainerDefinitions = currentTaskDefinition.containerDefinitions()
                                .stream()
                                .map(cd -> cd.name().equals(project.getContainerName())
                                                ? cd.toBuilder().image(imageUri).build()
                                                : cd)
                                .collect(Collectors.toList());

                RegisterTaskDefinitionResponse registerTaskDefResponse = ecsClient
                                .registerTaskDefinition(RegisterTaskDefinitionRequest.builder()
                                                .family(currentTaskDefinition.family())
                                                .volumes(currentTaskDefinition.volumes())
                                                .networkMode(currentTaskDefinition.networkMode())
                                                .requiresCompatibilities(
                                                                currentTaskDefinition.requiresCompatibilities())
                                                .cpu(currentTaskDefinition.cpu())
                                                .memory(currentTaskDefinition.memory())
                                                .executionRoleArn(currentTaskDefinition.executionRoleArn())
                                                .taskRoleArn(currentTaskDefinition.taskRoleArn())
                                                .containerDefinitions(newContainerDefinitions)
                                                .build());

                String newTaskDefinitionArn = registerTaskDefResponse.taskDefinition().taskDefinitionArn();
                log.info("New Task Definition registered: {}", newTaskDefinitionArn);

                ecsClient.updateService(UpdateServiceRequest.builder()
                                .cluster(project.getEcsClusterName())
                                .service(project.getEcsServiceName())
                                .taskDefinition(newTaskDefinitionArn)
                                .forceNewDeployment(true)
                                .build());
                log.info("Update service requested for: {}", project.getEcsServiceName());

                Optional<Deployment> previousSuccessfulDeployment = deploymentRepository
                                .findFirstByProjectAndStatusOrderByIdDesc(
                                                project, Deployment.DeploymentStatus.SUCCESS);

                deployment.setTaskDefinitionArn(newTaskDefinitionArn);
                deployment.setStatus(Deployment.DeploymentStatus.IN_PROGRESS);

                cat.cicd.entity.DeploymentStage deploymentStage = cat.cicd.entity.DeploymentStage.builder()
                                .name("deploy")
                                .status(cat.cicd.entity.DeploymentStage.StageStatus.IN_PROGRESS)
                                .build();
                deployment.addStage(deploymentStage);

                previousSuccessfulDeployment.ifPresent(deployment::setPreviousDeployment);

                return deploymentRepository.save(deployment);
        }

        @Transactional
        public Deployment rollbackToPreviousVersion(long deploymentId) {
                Deployment curDeployment = deploymentRepository.findById(deploymentId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Deployment not found with id: " + deploymentId));

                Project project = curDeployment.getProject();

                Deployment lastSuccessfulDeployment = deploymentRepository
                                .findFirstByProjectAndStatusOrderByIdDesc(project, Deployment.DeploymentStatus.SUCCESS)
                                .orElseThrow(() -> new RuntimeException(
                                                "No previous successful deployment found for service: "
                                                                + project.getEcsServiceName()));

                String rollbackTaskDefinitionArn = lastSuccessfulDeployment.getTaskDefinitionArn();
                log.info("Rolling back service {} to task definition {}", project.getEcsServiceName(),
                                rollbackTaskDefinitionArn);

                ecsClient.updateService(UpdateServiceRequest.builder()
                                .cluster(project.getEcsClusterName())
                                .service(project.getEcsServiceName())
                                .taskDefinition(rollbackTaskDefinitionArn)
                                .forceNewDeployment(true)
                                .build());
                log.info("Rollback requested for service: {}", project.getEcsServiceName());

                Deployment rollbackDeployment = new Deployment(project, lastSuccessfulDeployment.getGithubRunId(),
                                project.getEcsClusterName(), project.getEcsServiceName());
                rollbackDeployment.setTaskDefinitionArn(rollbackTaskDefinitionArn);
                rollbackDeployment.setStatus(Deployment.DeploymentStatus.ROLLED_BACK);
                rollbackDeployment.setPreviousDeployment(lastSuccessfulDeployment);

                return deploymentRepository.save(rollbackDeployment);
        }

        @Transactional
        public Project discoverAndSaveEcsInfo(long projectId) {
			Project project = projectRepository.findById(projectId)
					.orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

			log.info("Attempting to discover ECS info for project: {}", project.getName());
			TagFilter tagFilter = TagFilter.builder()
					.key("ProjectRepo")
					.values(project.getName())
					.build();

			GetResourcesRequest resourcesRequest = GetResourcesRequest.builder()
					.resourceTypeFilters("ecs:service")
					.tagFilters(tagFilter)
					.build();

			List<ResourceTagMapping> mappings = taggingApiClient.getResources(resourcesRequest)
					.resourceTagMappingList();

			if (mappings.isEmpty()) {
					throw new RuntimeException("No tagged ECS service found for project: " + project.getName());
			}
			if (mappings.size() > 1) {
					log.warn("Multiple tagged ECS services found for project: {}. Using the first one.", project.getName());
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
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Project not found with id: " + projectId));

                if (project.getEcsClusterName().isEmpty()) {
                        throw new IllegalArgumentException("Ecs Cluster name not found, Please synchronization first");
                }

                ListServicesRequest request = ListServicesRequest.builder().cluster(project.getEcsClusterName())
                                .build();
                return ecsClient.listServicesPaginator(request).stream()
                                .flatMap(response -> response.serviceArns().stream())
                                .collect(Collectors.toList());
        }
}
