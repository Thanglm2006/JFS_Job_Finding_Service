package com.example.JFS_Job_Finding_Service.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ApplicantRegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private String name;
    private Map<String, Object> resume;
}
