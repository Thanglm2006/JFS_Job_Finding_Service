package com.example.JFS_Job_Finding_Service.DTO.Schedule;

import lombok.Data;

@Data
public class UserOutAdmin {
    private String fullName;
    private String applicantId;
    private String employerId;
    private long userId;
    private String avatarUrl;
    private boolean active;
    private String role;

}
