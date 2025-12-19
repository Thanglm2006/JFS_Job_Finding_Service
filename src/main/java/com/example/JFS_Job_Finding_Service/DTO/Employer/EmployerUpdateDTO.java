package com.example.JFS_Job_Finding_Service.DTO.Employer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployerUpdateDTO {
    // User fields
    private String fullName;
    private String phoneNumber;
    private String address;
    private String gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    // Employer fields
    private String orgName;
    private String employerType;
    private String customType;
    private String taxCode;
    private String businessCode;
    private String companyWebsite;
    private String companyEmail;
    private String headquartersAddress;
    private String idCardNumber;

    private MultipartFile businessLicense;
    private MultipartFile idCardFront;
}