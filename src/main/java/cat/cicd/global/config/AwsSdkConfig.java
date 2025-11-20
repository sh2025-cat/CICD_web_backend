package cat.cicd.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;

@Configuration
public class AwsSdkConfig {

    @Bean
    public EcsClient ecsClient() {
        return EcsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

	@Bean
	public EcrClient ecrClient() {
		return EcrClient.builder()
				.credentialsProvider(DefaultCredentialsProvider.builder().build())
				.region(Region.AP_NORTHEAST_2)
				.build();
	}

	@Bean
	public CloudWatchClient cloudWatchClient() {
		return CloudWatchClient.builder()
				.credentialsProvider(DefaultCredentialsProvider.builder().build())
				.region(Region.AP_NORTHEAST_2)
				.build();
	}

	@Bean
	public ResourceGroupsTaggingApiClient resourceGroupsTaggingApiClient() {
		return ResourceGroupsTaggingApiClient.builder()
				.credentialsProvider(DefaultCredentialsProvider.builder().build())
				.region(Region.AP_NORTHEAST_2)
				.build();
	}
}
