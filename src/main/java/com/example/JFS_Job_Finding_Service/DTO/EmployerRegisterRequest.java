package com.example.JFS_Job_Finding_Service.DTO;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class EmployerRegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private String name;
    private String employerType;
}
