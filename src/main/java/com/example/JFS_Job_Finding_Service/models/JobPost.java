package com.example.JFS_Job_Finding_Service.models;

import com.example.JFS_Job_Finding_Service.models.Enum.JobType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "job_post")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class JobPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private String[] addresses;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] positions;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @Column(nullable = true, name = "workspace_picture", columnDefinition = "TEXT")
    private String workspacePicture;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "search_text", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String searchText;
}