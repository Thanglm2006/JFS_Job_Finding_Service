package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name= "job_taken")
@Getter @Setter
@AllArgsConstructor @Builder
@NoArgsConstructor
public class JobTaken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="job_id", nullable = false)
    private JobPost jobPost;
    @ManyToOne
    @JoinColumn(name="applicant_id", nullable = false)
    private Applicant applicant;
    @Column(name = "taken_at", nullable = false)
    private Instant takenAt= Instant.now();

}
