package com.example.JFS_Job_Finding_Service.DTO.Post;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class JobPostSummaryDTO {
    private String id;
    private String title;
    private String employerId;
    private long employerUserId;
    private String avatarUrl;
    private String employerName;
    private String salary;      // e.g., "1000 - 2000" or "Thương lượng"
    private String orgName;
    private String address;
    private String city;
    private String state;
    private String jobType;
    private Instant createdAt;
    private boolean isSaved;
    private boolean isApplied;
}