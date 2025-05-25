package com.example.JFS_Job_Finding_Service.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter @Setter
public class EmployerRegisterRequest {
    private String name;
    private String email;
    private String employerType;
    private String password;
    private Date dateOfBirth;
    private String gender;
    private String retypePass;

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
