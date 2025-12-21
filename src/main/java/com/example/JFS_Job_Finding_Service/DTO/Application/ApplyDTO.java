package com.example.JFS_Job_Finding_Service.DTO.Application;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ApplyDTO {
    private String jobId;
    private String position;
    private MultipartFile cv;
}
