package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Data;

@Data
public class reviewEmployerDTO {
    private String employerId;
    private long requestId;
    private String reason;
}
