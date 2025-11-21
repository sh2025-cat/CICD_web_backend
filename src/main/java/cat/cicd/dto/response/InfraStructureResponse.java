package cat.cicd.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record InfraStructureResponse(
        // 기본 서비스 상태
        String serviceName,
        String status,
        int desiredCount,
        int runningCount,
        int pendingCount,

        // 리소스 스펙
        String cpu,
        String memory,
        String taskDefinitionArn,

        // 컨테이너 상세
        String containerName,
        String image,
        List<String> environments,

        // 네트워크 보안 설정
        List<String> subnets,
        List<String> securityGroups,
        String assignPublicIp,

        // 네트워크 연결 정보
        String loadBalancerTargetGroup,
        Integer containerPort,

        // 권한
        String executionRoleArn,
        String taskRoleArn,

        // 관측성
        String logGroup,

        // 배포 전략
        Integer deploymentMaxPercent,
        Integer deploymentMinHealthyPercent,
        String capacityProviderStrategy,

        // 보안 변수
        List<String> secretKeys
) {
}