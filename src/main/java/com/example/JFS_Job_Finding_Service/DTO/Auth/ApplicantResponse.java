package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
public class ApplicantResponse {
    private String jobId;
    private String applicantId;
    private LocalDateTime interview;
    private String reason;

}
