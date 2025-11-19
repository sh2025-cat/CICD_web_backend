package cat.cicd.feature.deployment.service;

import cat.cicd.feature.deployment.dto.EcsDeployRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS ECS 배포 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcsService {

    private final EcsClient ecsClient;

    @Value("${aws.ecr.repository-uri}")
    private String ecrRepositoryUri;

    /**
     * ECS 서비스를 업데이트하여 새로운 이미지를 배포합니다.
     *
     * @param request 배포 요청 정보
     * @return 업데이트된 서비스 정보
     */
    public software.amazon.awssdk.services.ecs.model.Service updateService(EcsDeployRequest request) {
        DescribeServicesResponse describeServicesResponse = ecsClient.describeServices(
                builder -> builder.cluster(request.cluster()).services(request.service()));

        if (describeServicesResponse.services().isEmpty()) {
            throw new RuntimeException("서비스를 찾을 수 없습니다: " + request.service());
        }
        String currentTaskDefinitionArn = describeServicesResponse.services().get(0).taskDefinition();

        DescribeTaskDefinitionResponse describeTaskDefinitionResponse = ecsClient.describeTaskDefinition(
                builder -> builder.taskDefinition(currentTaskDefinitionArn));
        TaskDefinition currentTaskDefinition = describeTaskDefinitionResponse.taskDefinition();

        String newImage = ecrRepositoryUri + ":" + request.imageTag();
        List<ContainerDefinition> newContainerDefinitions = currentTaskDefinition.containerDefinitions().stream()
                .map(cd -> {
                    if (cd.name().equals(request.containerName())) {
                        return cd.toBuilder().image(newImage).build();
                    }
                    return cd;
                })
                .collect(Collectors.toList());

        RegisterTaskDefinitionRequest registerRequest = RegisterTaskDefinitionRequest.builder()
                .family(currentTaskDefinition.family())
                .volumes(currentTaskDefinition.volumes())
                .networkMode(currentTaskDefinition.networkMode())
                .requiresCompatibilities(currentTaskDefinition.requiresCompatibilities())
                .cpu(currentTaskDefinition.cpu())
                .memory(currentTaskDefinition.memory())
                .executionRoleArn(currentTaskDefinition.executionRoleArn())
                .taskRoleArn(currentTaskDefinition.taskRoleArn())
                .containerDefinitions(newContainerDefinitions)
                .build();

        RegisterTaskDefinitionResponse registerTaskDefinitionResponse = ecsClient.registerTaskDefinition(registerRequest);
        String newTaskDefinitionArn = registerTaskDefinitionResponse.taskDefinition().taskDefinitionArn();
        log.info("새로운 태스크 정의가 등록되었습니다: {}", newTaskDefinitionArn);

        UpdateServiceRequest updateServiceRequest = UpdateServiceRequest.builder()
                .cluster(request.cluster())
                .service(request.service())
                .taskDefinition(newTaskDefinitionArn)
                .forceNewDeployment(true)
                .build();

        UpdateServiceResponse updateServiceResponse = ecsClient.updateService(updateServiceRequest);
        log.info("서비스 업데이트를 요청했습니다: {}", request.service());

        return updateServiceResponse.service();
    }
}
