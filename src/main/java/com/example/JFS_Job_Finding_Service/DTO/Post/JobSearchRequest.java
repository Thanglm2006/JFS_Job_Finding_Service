package com.example.JFS_Job_Finding_Service.DTO.Post;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class JobSearchRequest {
    private String keyword;
    private String type;        // "FULL_TIME", "PART_TIME", "INTERNSHIP"
    private String address;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private int limit = 0;
    private int offset= 10;
}