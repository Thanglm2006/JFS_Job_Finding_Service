package com.example.JFS_Job_Finding_Service.Controller;
import com.example.JFS_Job_Finding_Service.DTO.PostingRequest;
import com.example.JFS_Job_Finding_Service.Services.CloudinaryService;
import com.example.JFS_Job_Finding_Service.Services.PendingJobPostService;
import com.example.JFS_Job_Finding_Service.Services.PostService;
import com.example.JFS_Job_Finding_Service.Services.SavedJobService;
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
    @Autowired
    private PendingJobPostService pendingJobPostService;
    @Autowired
    private SavedJobService savedJobService;

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
                String folderName = cloudinaryService.uploadFiles(workspacePictures, title + new Date().toString());
                postingRequest.setWorkSpacePicture(folderName);
            }
            return pendingJobPostService.addPost(headers.getFirst("token"), postingRequest);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/getSomePosts")
    @Operation(summary = "select 10 posts per time")
    public ResponseEntity<?> getSomePosts(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            return postService.getSomePosts(headers.getFirst("token"), page, size);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching posts: " + e.getMessage());
        }
    }

    @PostMapping("/acceptPost")
    @Operation(summary = "accept a post, only admin can do this")
    public ResponseEntity<?> acceptPost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("pendingId") Long pendingId
    ) {
        try {
            return pendingJobPostService.acceptPost(headers.getFirst("token"), pendingId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error accepting post: " + e.getMessage());
        }
    }
    @PostMapping("/savePost")
    @Operation(summary = "save a post, only applicant can do this")
    public ResponseEntity<?> savePost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") Long jobId
    ) {
        try {
            return savedJobService.saveJob(headers.getFirst("token"), jobId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving post: " + e.getMessage());
        }
    }
    @GetMapping("/getSavedPosts")
    @Operation(summary = "get saved posts, only applicant can do this")
    public ResponseEntity<?> getSavedPosts(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            return savedJobService.getSavedJobs(headers.getFirst("token"), page, size);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching saved posts: " + e.getMessage());
        }
    }

}
