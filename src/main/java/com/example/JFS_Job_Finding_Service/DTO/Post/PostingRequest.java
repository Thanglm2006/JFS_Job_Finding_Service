package com.example.JFS_Job_Finding_Service.DTO.Post;

import com.example.JFS_Job_Finding_Service.models.POJO.JobPosition;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
@Data
public class PostingRequest {
    private String title;

    private String description;
    private String requirements;
    private String responsibilities;
    private String advantages;
    private String extension; // Optional

    private String type; // Maps to JobType Enum
    private String[] addresses;
    private List<JobPosition> positions;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    private MultipartFile[] files;
}