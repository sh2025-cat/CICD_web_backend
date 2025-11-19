package cat.cicd.feature.pipeline.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Artifact {
    @Id
    @GeneratedValue
    private Long id;

    private String serviceName;
    private String imageTag;

    private String commitHash;
    private String commitMessage;

    private LocalDateTime builtAt;

    public String getFullEcrUrl(String registryBaseUrl) {
        return String.format("%s/%s:%s", registryBaseUrl, this.serviceName, this.imageTag);
    }
}