package com.example.JFS_Job_Finding_Service.models;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity
@Table(name = "saved_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class SavedJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobPost job;

    @Column(nullable = false, updatable = false)
    private Instant savedAt;

    public SavedJob(Applicant applicant, JobPost job) {
        this.applicant = applicant;
        this.job = job;
    }
}
