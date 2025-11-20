package cat.cicd.controller;

import cat.cicd.global.common.CommonResponse;
import cat.cicd.service.ECRService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.ecr.model.ListImagesResponse;

import java.util.List;

@Tag(name = "ECR Controller", description = "AWS ECR 관련 API")
@RestController
@RequestMapping("/api/ecr")
@RequiredArgsConstructor
public class EcrController {

    private final ECRService ecrService;

    @Operation(summary = "ECR 이미지 태그 목록 조회", description = "특정 ECR 리포지토리의 모든 이미지 태그 목록을 조회합니다.")
    @GetMapping("/images/{projectName}/tags")
    public ResponseEntity<CommonResponse<List<String>>> getImageTags(@PathVariable String projectName) {
		List<String> tags = ecrService.getImageTags(projectName);
        return ResponseEntity.ok(CommonResponse.of(tags));
    }

    @Operation(summary = "모든 ECR 리포지토리 목록 조회", description = "AWS 계정의 모든 ECR 리포지토리 이름 목록을 조회합니다.")
    @GetMapping("/repositories")
    public ResponseEntity<CommonResponse<List<String>>> listEcrRepositories() {
        List<String> repositories = ecrService.listRepositories();
        return ResponseEntity.ok(CommonResponse.of(repositories));
    }
}
