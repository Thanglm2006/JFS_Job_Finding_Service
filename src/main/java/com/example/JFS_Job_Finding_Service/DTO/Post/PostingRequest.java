package com.example.JFS_Job_Finding_Service.DTO.Post;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PostingRequest {
    private String title;

    private String description;

    private MultipartFile[] files;
    private String[] positions;
}