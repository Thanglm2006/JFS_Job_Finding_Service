package com.example.JFS_Job_Finding_Service.DTO.Employer;

import com.example.JFS_Job_Finding_Service.models.Enum.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerRequestInfoDTO {
    private Long requestId;
    private String employerId;
    private String orgName;
    private String fullName;
    private String gender;
    private String email;
    private String phone;
    private String employerType;
    private String customType;
    private String businessCode;
    private VerificationStatus status;
    private LocalDateTime createdAt;
    private String taxCode;
    private String companyWebsite;
    private String companyEmail;
    private String headquartersAddress;
    private String idCardNumber;
    private String businessLicenseUrl;
    private String idCardFrontUrl;
}