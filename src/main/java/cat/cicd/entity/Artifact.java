package cat.cicd.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_artifacts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repositoryName;

    @Column(nullable = false)
    private String imageTag;

    @Column(nullable = false, unique = true)
    private String imageUri;

    private String commitHash;
    private String commitMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Artifact(String repositoryName, String imageTag, String imageUri) {
        this.repositoryName = repositoryName;
        this.imageTag = imageTag;
        this.imageUri = imageUri;
    }
}