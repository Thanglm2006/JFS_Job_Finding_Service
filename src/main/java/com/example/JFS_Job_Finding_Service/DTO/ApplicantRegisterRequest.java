package com.example.JFS_Job_Finding_Service.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class ApplicantRegisterRequest {
    private String email;
    private String password;
    private String retypePass;
    private Date dateOfBirth;
    private String gender;
    private String name;
}
