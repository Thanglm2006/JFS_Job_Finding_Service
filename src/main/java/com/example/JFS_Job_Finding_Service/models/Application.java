package com.example.JFS_Job_Finding_Service.models;
import com.example.JFS_Job_Finding_Service.Services.ApplicationService;
import com.example.JFS_Job_Finding_Service.models.Enum.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity @Table(name="applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobPost job;

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;
    @Column(nullable = false, updatable = true)
    private String cv;

    @Column(updatable = false)
    private LocalDateTime interviewDate;
    @Column(nullable = false, updatable = false)
    private Instant appliedAt= Instant.now();
    @Column(nullable = false, updatable = true)
    private String position;
    @Column()
    private String reason;
    public Application(JobPost job, Applicant applicant, String position, String cv) {
        this.job = job;
        this.applicant = applicant;
        this.position = position;
        this.cv = cv;
    }
}
