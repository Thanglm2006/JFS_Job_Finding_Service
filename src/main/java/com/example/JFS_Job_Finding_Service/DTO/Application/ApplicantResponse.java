package com.example.JFS_Job_Finding_Service.DTO.Application;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicantResponse {
    private String jobId;
    private String applicantId;
    private LocalDateTime interview;
    private String reason;

}