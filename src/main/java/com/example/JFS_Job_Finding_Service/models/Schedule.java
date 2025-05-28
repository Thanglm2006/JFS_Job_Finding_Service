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
    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobPost job;
    @Column(name="start_time", nullable = false)
    private int startTime; // Start time in minutes from 0 to 1439 (0-23:59)
    @Column(name="end_time", nullable = false)
    private int endTime; // End time in minutes from 0 to 1439 (0-23:59)
    @Column(name="day", nullable = false)
    private String day; // Day of the week (e.g., "Monday", "Tuesday", etc.)
    @Column(name="description", nullable = false)
    private String description; // Description of the schedule
    @Column(name="created_at", nullable = false)
    private Instant createdAt= Instant.now();

}
