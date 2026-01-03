package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employer_id", referencedColumnName = "id", nullable = false)
    private Employer employer;

    @ManyToOne
    @JoinColumn(name = "applicant_id", referencedColumnName = "id", nullable = false)
    private Applicant applicant;
    @Column(name="interview_date")
    private LocalDateTime interviewDate;
    @Column(name="room")
    private String room;
    @Column
    private LocalDateTime createdAt;
}
