package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "job_shift")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPost job;

    @Column(name = "position_name", nullable = false)
    private String positionName;

    @Column(nullable = false)
    private String day;

    @Column(name = "start_time", nullable = false)
    private int startTime;

    @Column(name = "end_time", nullable = false)
    private int endTime;

    @Column(name = "max_quantity", nullable = false)
    private int maxQuantity;

    private String description;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
