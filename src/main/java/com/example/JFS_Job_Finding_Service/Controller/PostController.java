package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Post.JobSearchRequest;
import com.example.JFS_Job_Finding_Service.DTO.Post.PostingRequest;
import com.example.JFS_Job_Finding_Service.Services.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    // --- Creation & Management (Pending Posts) ---

    @Operation(summary = "Add a new post", description = "Post with title, description fields (JSON strings), and files.")
    @PostMapping(value = "/addPost", consumes = "multipart/form-data")
    public ResponseEntity<?> addPost(
            @RequestHeader("token") String token,
            @ModelAttribute PostingRequest postingRequest
    ) {
        return pendingJobPostService.addPost(token, postingRequest);
    }

    @PostMapping("/deletePendingPosts")
    @Operation(summary = "Delete pending posts", description = "Only employer or admin can use")
    public ResponseEntity<?> deletePendingPosts(
            @RequestHeader HttpHeaders headers,
            @RequestParam long pendingId
    ) {
        try{
            return pendingJobPostService.deletePendingPost(headers.getFirst("token"), pendingId);
        } catch (Exception e){
            return ResponseEntity.status(500).body("Error deleting pending post: " + e.getMessage());
        }
    }

    @GetMapping("/getSomePendingPosts")
    @Operation(summary = "Get list of pending posts (Simplified View)", description = "Select 10 pending posts per time, only admin/employer")
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

    @GetMapping("/getPendingPostDetail")
    @Operation(summary = "Get full details of a pending post", description = "Returns all fields including specific descriptions")
    public ResponseEntity<?> getPendingPostDetail(
            @RequestHeader HttpHeaders headers,
            @RequestParam("pendingId") long pendingId
    ) {
        try {
            return pendingJobPostService.getPendingJobPostDetail(headers.getFirst("token"), pendingId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching pending post detail: " + e.getMessage());
        }
    }

    @PostMapping("/rejectPost")
    @Operation(summary = "Reject a post", description = "Only admin can do this")
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
    @Operation(summary = "Accept a post", description = "Only admin can do this")
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

    // --- Active Posts & Search ---

    @GetMapping("/getSomePosts")
    @Operation(summary = "Get list of active posts (Simplified View)", description = "Select 10 posts per time")
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

    @GetMapping("/getSomePostsOfEmployer")
    @Operation(summary = "Get list of posts for a specific employer", description = "Simplified view for employer dashboard")
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

    @GetMapping("/findPost")
    @Operation(summary = "Full-text search with filters", description = "Search by keyword, type, address, salary range")
    public ResponseEntity<?> findPost(
            @RequestHeader HttpHeaders headers,
            @ModelAttribute JobSearchRequest searchDTO
    ) {
        try {
            return postService.fullTextSearchPosts(headers.getFirst("token"), searchDTO);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error finding post: " + e.getMessage());
        }
    }

    @GetMapping("/getPostDetail")
    @Operation(summary = "Get full details of an active post", description = "Returns all fields including specific descriptions")
    public ResponseEntity<?> getPostDetail(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId
    ) {
        try {
            return postService.getJobPostDetail(headers.getFirst("token"), jobId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching post detail: " + e.getMessage());
        }
    }

    @PostMapping("/deletePost")
    @Operation(summary = "Delete an active post", description = "Only admin and employer can do this")
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
    @Operation(summary = "Save a post", description = "Only applicant can do this")
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
    @Operation(summary = "Unsave a post", description = "Only applicant can do this")
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

    @GetMapping("/getSavedPosts")
    @Operation(summary = "Get saved posts", description = "Only applicant can do this")
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

    @GetMapping("/getAppliedPosts")
    @Operation(summary = "Get applied posts", description = "Only applicant can do this")
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
    @GetMapping("/getPendingPostsOfEmployer")
    @Operation(summary = "Get list of pending posts for logged-in Employer", description = "Employer dashboard view")
    public ResponseEntity<?> getPendingPostsOfEmployer(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            return pendingJobPostService.getPendingPostsForEmployer(headers.getFirst("token"), page, size);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching employer pending posts: " + e.getMessage());
        }
    }
}