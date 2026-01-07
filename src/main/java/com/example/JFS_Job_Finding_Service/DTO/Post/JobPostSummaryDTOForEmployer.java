
package com.example.JFS_Job_Finding_Service.DTO.Post;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;
import java.util.List;
@Data
@Builder
public class JobPostSummaryDTOForEmployer {
    private String id;
    private String title;
    private String employerId;
    private long employerUserId;
    private String employerName;
    private String status;
    private String salary;      // e.g., "1000 - 2000" or "Thương lượng"
    private String orgName;
    private String jobType;
    private Instant createdAt;
    private String avatarUrl;
    private boolean isSaved;
    private boolean isApplied;
    private List<Map<String, Object>> applicants;
    private int numberOfApplicant;
}