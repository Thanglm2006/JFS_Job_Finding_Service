package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "report_on_post")
@NoArgsConstructor
@Getter @Setter
@AllArgsConstructor
public class ReportOnPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;
    @ManyToOne
    @JoinColumn(name="reported_post", nullable = false)
    private JobPost reportedPost;
    @Column(nullable = false)
    private String reason;
    @Column(nullable = false)
    private String details;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
