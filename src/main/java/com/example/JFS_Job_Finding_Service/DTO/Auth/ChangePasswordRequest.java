package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String email;
    private String code;
    private String newPassword;
    private String confirmPassword;
}