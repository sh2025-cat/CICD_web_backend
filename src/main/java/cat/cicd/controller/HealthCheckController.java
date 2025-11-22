package cat.cicd.controller;

import cat.cicd.global.common.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public ResponseEntity<CommonResponse<Void>> healthCheck() {
        return ResponseEntity.ok().build();
    }
}
