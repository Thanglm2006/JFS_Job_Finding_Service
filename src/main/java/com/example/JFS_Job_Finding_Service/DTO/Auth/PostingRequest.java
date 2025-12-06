package com.example.JFS_Job_Finding_Service.DTO.Auth;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PostingRequest {
    private String title;
    private Map<String, Object> description;
    private String workSpacePicture;
}
