package com.example.JFS_Job_Finding_Service.DTO.Auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailDTO {
    private String email;
    private String code;
}
