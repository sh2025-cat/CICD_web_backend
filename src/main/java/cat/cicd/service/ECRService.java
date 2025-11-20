package cat.cicd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;
import software.amazon.awssdk.services.ecr.paginators.DescribeRepositoriesIterable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ECRService {

    private final EcrClient ecrClient;

    public List<String> getImageTags(String projectName) {
        try {
            ListImagesRequest request = ListImagesRequest.builder()
                    .repositoryName(projectName)
                    .build();

            ListImagesResponse response = ecrClient.listImages(request);

            return response.imageIds().stream()
                    .map(ImageIdentifier::imageTag)
                    .toList();
        } catch (EcrException e) {
            throw new RuntimeException("ECR 접근 실패: " + e.getMessage(), e);
        }
    }

    public List<String> listRepositories() {
		DescribeRepositoriesIterable paginator = ecrClient.describeRepositoriesPaginator();
        return paginator.stream()
                .flatMap(response -> response.repositories().stream())
                .map(Repository::repositoryName)
                .collect(Collectors.toList());
    }
}
