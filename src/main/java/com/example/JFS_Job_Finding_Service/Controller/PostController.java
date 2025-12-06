package com.example.JFS_Job_Finding_Service.Controller;
import com.example.JFS_Job_Finding_Service.DTO.Auth.PostingRequest;
import com.example.JFS_Job_Finding_Service.Services.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.Map;


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
    @Autowired
    private ApplicationService applicationService;

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
    @GetMapping("/getSomePendingPosts")
    @Operation(summary = "select 10 pending posts per time, only admin can do this")
    public ResponseEntity<?> getSomePendingPosts(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            return pendingJobPostService.getSomePendingPosts(headers.getFirst("token"), page, size);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching pending posts: " + e.getMessage());
        }
    }
    @GetMapping("/getSomePostsOfEmployer")
    @Operation(summary = "select 10 posts per time for employer")
    public ResponseEntity<?> getSomePostsForEmployer(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            return postService.getSomePostOfEmployer(headers.getFirst("token"), page, size);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching posts for employer: " + e.getMessage());
        }
    }
    @PostMapping("/rejectPost")
    @Operation(summary = "reject a post, only admin can do this")
    public ResponseEntity<?> rejectPost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("pendingId") long pendingId
    ) {
        try {
            return pendingJobPostService.rejectPost(headers.getFirst("token"), pendingId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error rejecting post: " + e.getMessage());
        }
    }
    @PostMapping("/acceptPost")
    @Operation(summary = "accept a post, only admin can do this")
    public ResponseEntity<?> acceptPost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("pendingId") long pendingId
    ) {
        try {
            return pendingJobPostService.acceptPost(headers.getFirst("token"), pendingId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error accepting post: " + e.getMessage());
        }
    }
    @PostMapping("/deletePost")
    @Operation(summary = "delete a post, only admin and employer can do this")
    public ResponseEntity<?> deletePost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId
    ) {
        try {
            return postService.deletePost(headers.getFirst("token"), jobId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting post: " + e.getMessage());
        }
    }
    @GetMapping("/savePost")
    @Operation(summary = "save a post, only applicant can do this")
    public ResponseEntity<?> savePost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId
    ) {
        try {
            return savedJobService.saveJob(headers.getFirst("token"), jobId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving post: " + e.getMessage());
        }
    }
    @GetMapping("/unSavePost")
    @Operation(summary = "unsave a post, only applicant can do this")
    public ResponseEntity<?> unsavePost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId
    ) {
        try {
            return savedJobService.unSaveJob(headers.getFirst("token"), jobId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error unsaving post: " + e.getMessage());
        }
    }
    @GetMapping("/apply")
    @Operation(summary = "apply for a post, only applicant can do this")
    public ResponseEntity<?> apply(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId,
            @RequestParam("position") String position
    ) {
        try {
            return applicationService.applyForJob(headers.getFirst("token"), jobId,position);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error applying for post: " + e.getMessage());
        }
    }
    @GetMapping("/unApply")
    @Operation(summary = "unapply for a post, only applicant can do this")
    public ResponseEntity<?> unApply(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId
    ) {
        try {
            return applicationService.unApplyForJob(headers.getFirst("token"), jobId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error unapplying for post: " + e.getMessage());
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
    @GetMapping("/findPost")
    @Operation(summary = "find a post by pattern")
    public ResponseEntity<?> findPost(
            @RequestHeader HttpHeaders headers,
            @RequestParam("pattern") String pattern
    ) {
        try {
            return postService.fullTextSearchPosts(headers.getFirst("token"), pattern);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error finding post: " + e.getMessage());
        }
    }
    @GetMapping("/getAppliedPosts")
    @Operation(summary = "get applied posts, only applicant can do this")
    public ResponseEntity<?> getAppliedPosts(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            return postService.getAppliedJobs(headers.getFirst("token"), page, size);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching applied posts: " + e.getMessage());
        }
    }

}
