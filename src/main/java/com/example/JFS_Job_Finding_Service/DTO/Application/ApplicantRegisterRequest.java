package com.example.JFS_Job_Finding_Service.DTO.Application;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
