package com.example.JFS_Job_Finding_Service.DTO.Post;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

@Data
public class PostingRequest {
    private String title;

    // These fields are expected to be JSON Strings coming from the frontend
    private String description;
    private String requirements;
    private String responsibilities;
    private String advantages;
    private String extension; // Optional

    private String type; // Maps to JobType Enum
    private String[] addresses;
    private String[] positions;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    private MultipartFile[] files;
}