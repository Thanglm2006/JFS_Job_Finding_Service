package com.example.JFS_Job_Finding_Service.DTO.Auth;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfile {
    private String name;
    private String phoneNumber;
    private String location;
    private String profilePictureUrl;
    private String employerType;
    private String gender;
    private LocalDate dateOfBirth;
    private Map<String,Object> resume;
    private Instant updatedAt = Instant.now();
    private LocalDateTime lastLoginTime;

    public UpdateProfile(String name, String phoneNumber, String location) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.location = location;
    }
    public UpdateProfile(String name, String phoneNumber, String location, String gender) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.gender = gender;
    }
    public UpdateProfile(String name, String phoneNumber, String location, String profilePictureUrl, String employerType) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.profilePictureUrl = profilePictureUrl;
        this.employerType = employerType;
    }
}
