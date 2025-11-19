package cat.cicd.feature.deployment.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.EcrException;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.ListImagesRequest;
import software.amazon.awssdk.services.ecr.model.ListImagesResponse;

import java.util.List;

@Service
public class ECRService {

    private final EcrClient ecrClient;

    public ECRService() {
        this.ecrClient = EcrClient.create();
    }

    public List<String> getImageTags(String repositoryName) {
        try {
            ListImagesRequest request = ListImagesRequest.builder()
                    .repositoryName(repositoryName)
                    .build();

            ListImagesResponse response = ecrClient.listImages(request);

            return response.imageIds().stream()
                    .map(ImageIdentifier::imageTag)
                    .toList();
        } catch (EcrException e) {
            throw new RuntimeException("ECR 접근 실패: " + e.getMessage(), e);
        }
    }
}
