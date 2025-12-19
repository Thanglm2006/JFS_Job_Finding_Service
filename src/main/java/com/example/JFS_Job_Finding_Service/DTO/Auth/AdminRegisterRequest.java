package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Data;

@Data
public class AdminRegisterRequest {
    private String secretPass;
    private String fullName;
    private String email;
    private String password;
}