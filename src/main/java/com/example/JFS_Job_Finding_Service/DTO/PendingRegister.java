package com.example.JFS_Job_Finding_Service.DTO;

import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Employer;
import com.example.JFS_Job_Finding_Service.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegister {
    private User user;
    private Employer employer;
    private Applicant applicant;
    private String code;
    private long expireTime;
}
