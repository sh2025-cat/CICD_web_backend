package cat.cicd.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.EcsClient;

/**
 * AWS SDK 관련 설정을 위한 클래스입니다.
 */
@Configuration
public class AwsSdkConfig {

    /**
     * AWS ECS 클라이언트를 빈으로 등록합니다.
     * <p>
     * 기본 자격 증명 공급자(환경 변수, 시스템 속성, 자격 증명 파일 등)를 사용하고,
     * 리전은 서울(ap-northeast-2)로 설정합니다.
     * </p>
     *
     * @return EcsClient 인스턴스
     */
    @Bean
    public EcsClient ecsClient() {
        return EcsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .region(Region.AP_NORTHEAST_2)
                .build();
    }
}
