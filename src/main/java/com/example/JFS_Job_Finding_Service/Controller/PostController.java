package com.example.JFS_Job_Finding_Service.Controller;
import com.example.JFS_Job_Finding_Service.DTO.PostingRequest;
import com.example.JFS_Job_Finding_Service.Services.CloudinaryService;
import com.example.JFS_Job_Finding_Service.Services.PostService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;


@Controller
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;
    @Autowired
    private CloudinaryService cloudinaryService;

    @Operation(summary = "Add a new post", description = "to post, you need to specify the Header with token and the body with title, description, and workspace picture")
    @PostMapping("/addPost")
    public ResponseEntity<?> addPost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("title") String title,
            @RequestParam("description") String descriptionJson,
            @RequestParam(value = "files", required = false) MultipartFile[] workspacePictures
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> descriptionMap = mapper.readValue(descriptionJson, new TypeReference<>() {});

            PostingRequest postingRequest = new PostingRequest();
            postingRequest.setDescription(descriptionMap);
            System.out.println(postingRequest.getDescription());
            postingRequest.setTitle(title);

            if (workspacePictures != null) {
                System.out.println("exist workspace pictures");
                String folderName = cloudinaryService.uploadFiles(workspacePictures, title + new Date().toString());
                postingRequest.setWorkSpacePicture(folderName);
            }

            return postService.addPost(headers.getFirst("token"), postingRequest);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}
