package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Post.PostingRequest;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.Map;

@Service
public class PostService {
    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<?> addPost(String token, PostingRequest postingRequest) {
        Map<String, Object> response = new HashMap<>();
        if(!jwtUtil.validateToken(token,jwtUtil.extractEmail(token))){
            response.put("status", "fail");
            response.put("message", "Invalid Token");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)){
            response.put("status", "fail");
            response.put("message", "not a employer to post");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        JobPost jobPost = new JobPost();
        jobPost.setTitle(postingRequest.getTitle());
        jobPost.setEmployer(jwtUtil.getEmployer(token));
        jobPost.setDescription(postingRequest.getDescription());
        jobPost.setWorkspacePicture(postingRequest.getWorkSpacePicture());
        jobPostRepository.save(jobPost);
        response.put("status", "success");
        response.put("message", "Post added successfully");
        response.put("jobPost", jobPost);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
