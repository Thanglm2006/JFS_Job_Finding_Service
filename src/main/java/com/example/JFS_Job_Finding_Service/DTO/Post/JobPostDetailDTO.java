package com.example.JFS_Job_Finding_Service.DTO.Post;

import com.example.JFS_Job_Finding_Service.models.POJO.JobPosition;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class JobPostDetailDTO {
    private String id;
    private String title;
    private String orgName;
    private String orgAvatar;
    private String employerId;
    private long employerUserId;
    private String employerName;
    // Detailed JSON fields
    private Map<String, Object> description;
    private Map<String, Object> requirements;
    private Map<String, Object> responsibilities;
    private Map<String, Object> advantages;
    private Map<String, Object> extension;

    private List<JobPosition> positions;
    private String[] addresses;
    private String jobType;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryString; // Formatted string

    private List<String> workspacePictures;
    private Instant createdAt;

    private boolean isSaved;
    private boolean isApplied;
}