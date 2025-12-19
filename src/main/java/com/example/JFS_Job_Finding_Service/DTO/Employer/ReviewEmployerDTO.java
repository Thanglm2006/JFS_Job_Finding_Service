package com.example.JFS_Job_Finding_Service.DTO.Employer;

import lombok.Data;

@Data
public class ReviewEmployerDTO {
    private Long employerId;
    private Long requestId;
    private String reason; // Required if rejecting
}