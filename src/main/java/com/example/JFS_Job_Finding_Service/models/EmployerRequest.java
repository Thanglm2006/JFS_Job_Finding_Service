package com.example.JFS_Job_Finding_Service.models;

import com.example.JFS_Job_Finding_Service.models.Enum.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "employer_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employer", referencedColumnName = "id", nullable = false)
    private Employer employer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}