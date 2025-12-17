package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter @Setter
public class EmployerRegisterRequest {
    private String name;
    private String email;
    private String employerType;
    private String customType;
    private String password;
    private LocalDate dateOfBirth;
    private String gender;
    private String retypePass;
    private String org;
    @Override
    public String toString() {
        return "EmployerRegisterRequest{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", employerType='" + employerType + '\'' +
                ", password='" + password + '\'' +
                ", retypePass='" + retypePass + '\'' +
                '}';
    }
}
