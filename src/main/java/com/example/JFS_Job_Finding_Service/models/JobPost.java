package com.example.JFS_Job_Finding_Service.models;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "job_post")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class JobPost {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(nullable = false)
    private String title;

    @Getter
    @ManyToOne
    @JoinColumn(name = "employer_id", referencedColumnName = "id", nullable = false)
    private Employer employer;

    @JdbcTypeCode(SqlTypes.JSON)  // ✅ Use correct annotation for JSONB in Hibernate 6
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> description;
    @Column(nullable = true, name="workspace_picture")
    private String workspacePicture;

    @Column(nullable = false)
    private Instant createdAt=Instant.now();
    public JobPost(String title, Employer employer, Map<String, Object> description) {
        this.title = title;
        this.employer = employer;
        this.description = description;
    }

    public JobPost(String title, Employer employer, Map<String, Object> description, String workspacePicture) {
        this.title = title;
        this.employer = employer;
        this.description = description;
        this.workspacePicture = workspacePicture;
    }
}
