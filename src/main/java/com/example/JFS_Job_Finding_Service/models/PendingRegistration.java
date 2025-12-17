package com.example.JFS_Job_Finding_Service.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PendingRegistration {
    private User user;
    private Employer employer;
    private Applicant applicant;
    private String verificationCode;
    private LocalDateTime expiryTime;
}