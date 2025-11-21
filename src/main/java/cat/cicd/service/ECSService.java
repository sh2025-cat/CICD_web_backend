package cat.cicd.service;

import cat.cicd.dto.response.InfraStructureResponse;
import cat.cicd.entity.Deployment;
import cat.cicd.entity.DeploymentStage;
import cat.cicd.entity.Project;

import cat.cicd.repository.DeploymentRepository;
import cat.cicd.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter;

import java.util.ArrayList;
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

    public ECSService(EcsClient ecsClient, ProjectRepository projectRepository, DeploymentRepository deploymentRepository) {
        this.ecsClient = ecsClient;
        this.taggingApiClient = ResourceGroupsTaggingApiClient.create();
        this.projectRepository = projectRepository;
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional
    public Deployment deployNewVersion(Long projectId, String imageTag) { // deploymentId 대신 projectId를 받음
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // 1. 새 배포 객체 생성 (이 시점에 DB에 저장하여 ID 생성)
        Deployment newDeployment = Deployment.builder()
                .project(project)
                .imageTag(imageTag)
                .status(Deployment.DeploymentStatus.PENDING)
                .build();

        newDeployment = deploymentRepository.save(newDeployment);

        return processDeployment(newDeployment, project, imageTag);
    }

    @Transactional
    protected Deployment processDeployment(Deployment deployment, Project project, String imageTag) {
        DescribeServicesResponse describeServicesResponse = ecsClient.describeServices(
                b -> b.cluster(project.getEcsClusterName()).services(project.getEcsServiceName()));
        if (describeServicesResponse.services().isEmpty()) {
            throw new RuntimeException("ECS service not found: " + project.getEcsServiceName());
        }
        String currentTaskDefinitionArn = describeServicesResponse.services().getFirst().taskDefinition();
        TaskDefinition currentTaskDefinition = ecsClient.describeTaskDefinition(
                b -> b.taskDefinition(currentTaskDefinitionArn)).taskDefinition();

        List<ContainerDefinition> existingContainers = currentTaskDefinition.containerDefinitions();

        if (existingContainers.isEmpty()) {
            throw new RuntimeException("컨테이너가 정의되지 않은 Task Definition입니다.");
        }

        ContainerDefinition mainContainer = existingContainers.getFirst();

        String currentImageUri = mainContainer.image();
        String repositoryUri;

        int tagIndex = currentImageUri.lastIndexOf(":");
        if (tagIndex > 0) {
            repositoryUri = currentImageUri.substring(0, tagIndex);
        } else {
            repositoryUri = currentImageUri;
        }

        String imageUri = repositoryUri + ":" + imageTag;
        log.info("Deploying with Image URI: {}", imageUri);

        ContainerDefinition updatedMainContainer = mainContainer.toBuilder()
                .image(imageUri)
                .build();

        List<ContainerDefinition> newContainerDefinitions = new ArrayList<>(existingContainers);
        newContainerDefinitions.set(0, updatedMainContainer);

        RegisterTaskDefinitionResponse registerTaskDefResponse = ecsClient
                .registerTaskDefinition(RegisterTaskDefinitionRequest.builder()
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
                .cluster(project.getEcsClusterName())
                .service(project.getEcsServiceName())
                .taskDefinition(newTaskDefinitionArn)
                .forceNewDeployment(true)
                .build());
        log.info("Update service requested for: {}", project.getEcsServiceName());

        Optional<Deployment> previousSuccessfulDeployment = deploymentRepository
                .findFirstByProjectAndStatusOrderByIdDesc(project, Deployment.DeploymentStatus.SUCCESS);

        deployment.setTargetCluster(project.getEcsClusterName());
        deployment.setTargetService(project.getEcsServiceName());
        deployment.setTaskDefinitionArn(newTaskDefinitionArn);
        deployment.setStatus(Deployment.DeploymentStatus.IN_PROGRESS);

        Optional<Deployment> lastSuccess = deploymentRepository
                .findFirstByProjectAndStatusOrderByIdDesc(project, Deployment.DeploymentStatus.SUCCESS);
        lastSuccess.ifPresent(deployment::setPreviousDeployment);

        deployment.setTargetCluster(project.getEcsClusterName());
        deployment.setTargetService(project.getEcsServiceName());
        deployment.setTaskDefinitionArn(newTaskDefinitionArn);
        deployment.setStatus(Deployment.DeploymentStatus.IN_PROGRESS);

        DeploymentStage deploymentStage = DeploymentStage.builder()
                .name("deploy")
                .status(DeploymentStage.StageStatus.IN_PROGRESS)
                .build();
        deployment.addStage(deploymentStage);

        previousSuccessfulDeployment.ifPresent(deployment::setPreviousDeployment);

        return deploymentRepository.save(deployment);
    }

    @Transactional
    public Deployment rollbackToPreviousVersion(long currentDeploymentId) {
        Deployment currentDeployment = deploymentRepository.findById(currentDeploymentId)
                .orElseThrow(() -> new RuntimeException("Deployment not found"));

        Project project = currentDeployment.getProject();

        List<Deployment> recentSuccesses = deploymentRepository.findByProjectAndStatusOrderByCreatedAtDesc(
                project,
                Deployment.DeploymentStatus.SUCCESS,
                PageRequest.of(0, 2)
        );

        Deployment stableDeployment;

        if (recentSuccesses.isEmpty()) {
            throw new RuntimeException("롤백할 수 있는 이전 성공 배포가 없습니다.");
        }

        if (currentDeployment.getStatus() == Deployment.DeploymentStatus.SUCCESS) {
            if (recentSuccesses.size() < 2) {
                throw new RuntimeException("이전 버전의 배포 이력이 없습니다.");
            }
            stableDeployment = recentSuccesses.get(1);
        } else {
            stableDeployment = recentSuccesses.getFirst();
        }

        String rollbackTaskDefinitionArn = stableDeployment.getTaskDefinitionArn();
        log.info("Rolling back to Task Definition: {}", rollbackTaskDefinitionArn);

        ecsClient.updateService(UpdateServiceRequest.builder()
                .cluster(project.getEcsClusterName())
                .service(project.getEcsServiceName())
                .taskDefinition(rollbackTaskDefinitionArn)
                .forceNewDeployment(true)
                .build());

        Deployment rollbackRecord = Deployment.builder()
                .project(project)
                .taskDefinitionArn(rollbackTaskDefinitionArn)
                .targetCluster(project.getEcsClusterName())
                .targetService(project.getEcsServiceName())
                .status(Deployment.DeploymentStatus.SUCCESS)
                .build();

        return deploymentRepository.save(rollbackRecord);
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

    @Transactional(readOnly = true)
    public InfraStructureResponse getInfraStructure(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found"));

        String clusterName = (deployment.getTargetCluster() != null && !deployment.getTargetCluster().isEmpty())
                ? deployment.getTargetCluster()
                : deployment.getProject().getEcsClusterName();

        String serviceName = (deployment.getTargetService() != null && !deployment.getTargetService().isEmpty())
                ? deployment.getTargetService()
                : deployment.getProject().getEcsServiceName();

        if (clusterName == null || clusterName.trim().isEmpty()) {
            throw new IllegalStateException("ECS Cluster name is missing for deployment: " + deploymentId);
        }
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalStateException("ECS Service name is missing for deployment: " + deploymentId);
        }

        DescribeServicesRequest serviceReq = DescribeServicesRequest.builder()
                .cluster(clusterName)
                .services(serviceName)
                .build();

        DescribeServicesResponse serviceRes = ecsClient.describeServices(serviceReq);

        if (serviceRes.services().isEmpty()) {
            throw new RuntimeException("ECS Service not found in AWS: " + serviceName);
        }

        software.amazon.awssdk.services.ecs.model.Service service = serviceRes.services().getFirst();

        String taskDefArn = service.taskDefinition();
        DescribeTaskDefinitionRequest taskDefReq = DescribeTaskDefinitionRequest.builder()
                .taskDefinition(taskDefArn)
                .build();

        TaskDefinition taskDefinition = ecsClient.describeTaskDefinition(taskDefReq).taskDefinition();

        if (taskDefinition.containerDefinitions().isEmpty()) {
            throw new RuntimeException("No container definitions found in task definition: " + taskDefArn);
        }
        ContainerDefinition container = taskDefinition.containerDefinitions().getFirst();
        AwsVpcConfiguration vpcConfig = service.networkConfiguration().awsvpcConfiguration();

        String targetGroupArn = null;
        Integer containerPort = null;
        if (!service.loadBalancers().isEmpty()) {
            LoadBalancer lb = service.loadBalancers().getFirst();
            targetGroupArn = lb.targetGroupArn();
            containerPort = lb.containerPort();
        }

        String logGroup = null;
        if (container.logConfiguration() != null && container.logConfiguration().options() != null) {
            logGroup = container.logConfiguration().options().get("awslogs-group");
        }

        Integer maxPercent = service.deploymentConfiguration() != null ? service.deploymentConfiguration().maximumPercent() : null;
        Integer minHealthyPercent = service.deploymentConfiguration() != null ? service.deploymentConfiguration().minimumHealthyPercent() : null;

        String capacityProviderStrategy = service.capacityProviderStrategy().stream()
                .map(cp -> cp.capacityProvider() + "(weight:" + cp.weight() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("DEFAULT");

        List<String> secretKeys = container.secrets().stream()
                .map(Secret::name)
                .toList();

        return InfraStructureResponse.builder()
                .serviceName(service.serviceName())
                .status(service.status())
                .desiredCount(service.desiredCount())
                .runningCount(service.runningCount())
                .pendingCount(service.pendingCount())
                .cpu(taskDefinition.cpu())
                .memory(taskDefinition.memory())
                .taskDefinitionArn(taskDefArn)
                .containerName(container.name())
                .image(container.image())
                .environments(container.environment().stream()
                        .map(kv -> kv.name() + "=" + kv.value())
                        .toList())
                .subnets(vpcConfig != null ? vpcConfig.subnets() : List.of())
                .securityGroups(vpcConfig != null ? vpcConfig.securityGroups() : List.of())
                .assignPublicIp(vpcConfig != null ? vpcConfig.assignPublicIpAsString() : "UNKNOWN")
                .loadBalancerTargetGroup(targetGroupArn)
                .containerPort(containerPort)
                .executionRoleArn(taskDefinition.executionRoleArn())
                .taskRoleArn(taskDefinition.taskRoleArn())
                .logGroup(logGroup)
                .deploymentMaxPercent(maxPercent)
                .deploymentMinHealthyPercent(minHealthyPercent)
                .capacityProviderStrategy(capacityProviderStrategy)
                .secretKeys(secretKeys)
                .build();
    }
}
