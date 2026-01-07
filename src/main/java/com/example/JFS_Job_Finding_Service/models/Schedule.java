package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPost job;

    @Column(name="start_time", nullable = false)
    private int startTime;

    @Column(name="end_time", nullable = false)
    private int endTime;

    @Column(name="day", nullable = false)
    private String day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private JobShift jobShift;

    @Column(name="description", nullable = false)
    private String description;

    @Column(name="created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}