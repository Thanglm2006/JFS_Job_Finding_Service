package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
public class ApplicantRegisterRequest {
    private String email;
    private String password;
    private String retypePass;
    private LocalDate dateOfBirth;
    private String gender;
    private String name;
}
