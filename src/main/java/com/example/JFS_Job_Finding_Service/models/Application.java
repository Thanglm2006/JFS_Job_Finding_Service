package com.example.JFS_Job_Finding_Service.models;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

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

    @Column(nullable = false)
    private String status = "Pending";

    @Column(nullable = false, updatable = false)
    private Instant appliedAt= Instant.now();
    @Column(nullable = false, updatable = true)
    private String position;
    public Application(JobPost job, Applicant applicant, String status, String position) {
        this.job = job;
        this.applicant = applicant;
        this.status = status;
        this.position= position;
    }

    public Application(JobPost job, Applicant applicant, String position) {
        this.job = job;
        this.applicant = applicant;
        this.position = position;
    }
}
