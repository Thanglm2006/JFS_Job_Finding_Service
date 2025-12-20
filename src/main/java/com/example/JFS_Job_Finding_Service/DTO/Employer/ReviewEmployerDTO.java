package com.example.JFS_Job_Finding_Service.DTO.Employer;

import lombok.Data;

@Data
public class ReviewEmployerDTO {
    private String employerId;
    private Long requestId;
    private String reason; // Required if rejecting
}