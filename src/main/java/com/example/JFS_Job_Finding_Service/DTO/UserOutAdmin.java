package com.example.JFS_Job_Finding_Service.DTO;

import lombok.Data;

@Data
public class UserOutAdmin {
    private String fullName;
    private String applicantId;
    private String employerId;
    private String avatarUrl;
    private boolean active;
    private String role;

}
