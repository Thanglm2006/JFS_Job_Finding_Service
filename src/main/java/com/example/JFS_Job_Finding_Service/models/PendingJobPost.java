package com.example.JFS_Job_Finding_Service.models;
import com.example.JFS_Job_Finding_Service.models.Enum.JobType;
import com.example.JFS_Job_Finding_Service.models.POJO.JobPosition;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "pending_job_post")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PendingJobPost {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String title;

    @Getter
    @ManyToOne
    @JoinColumn(name = "employer_id", referencedColumnName = "id", nullable = false)
    private Employer employer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_description", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> jobDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> requirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> responsibilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> advantages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> extension;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", columnDefinition = "job_type", nullable = false)
    private JobType type;
    @Column(nullable = true, name = "workspace_picture")
    private String workspacePicture;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<JobPosition> positions;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private String[] addresses;
    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

}