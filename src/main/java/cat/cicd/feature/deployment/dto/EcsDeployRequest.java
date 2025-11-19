package cat.cicd.feature.deployment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * ECS 배포 요청 정보를 담는 레코드입니다.
 *
 * @param cluster      ECS 클러스터 이름
 * @param service      ECS 서비스 이름
 * @param imageTag     새로 배포할 도커 이미지 태그
 * @param containerName 업데이트할 컨테이너 이름
 */
public record EcsDeployRequest(
    @NotBlank String cluster,
    @NotBlank String service,
    @NotBlank String imageTag,
    @NotBlank String containerName
) {}
