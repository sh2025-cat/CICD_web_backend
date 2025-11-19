package cat.cicd.feature.pipeline.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class Deployment {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private Artifact artifact;

    private String status;
    private String envType;
    private LocalDateTime deployedAt;
}
