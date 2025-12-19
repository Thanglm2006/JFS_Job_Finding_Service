package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Data;

@Data
public class CheckPermissionRequest {
    private String token;
    private String role;
}