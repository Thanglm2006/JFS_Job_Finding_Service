package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.models.SavedJob;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.repository.SavedJobRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SavedJobService {
    @Autowired
    SavedJobRepository savedJobRepository;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    private JobPostRepository jobPostRepository;

    public ResponseEntity<?> saveJob(String token, Long jobId) {

        Map<String, Object> response = new HashMap<>();

        if (!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Unauthorized access");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            response.put("status", "fail");
            response.put("message", "Applicant not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        JobPost jobPost = jobPostRepository.findById(jobId).get();
        SavedJob savedJob = new SavedJob(applicant, jobPost);
        savedJobRepository.save(savedJob);

        response.put("status", "success");
        response.put("message", "Job saved successfully");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    public ResponseEntity<?> getSavedJobs(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();

        if (!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Unauthorized access");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            response.put("status", "fail");
            response.put("message", "Applicant not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "SavedAt"));
        Page<SavedJob> savedJobsPage = savedJobRepository.findByApplicant(applicant, pageable);
        List<Map<String, Object>> posts= savedJobsPage.getContent().stream().map(savedJob -> {
            JobPost jobPost = savedJob.getJob();
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("description", jobPost.getDescription());
            postData.put("createdAt", jobPost.getCreatedAt());
            postData.put("isSaved", true);
            postData.put("employerName", jobPost.getEmployer() != null ? jobPost.getEmployer().getFullName() : "Unknown");
            postData.put("employerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            postData.put("workspacePicture", jobPost.getWorkspacePicture() != null ? jobPost.getWorkspacePicture() : "No picture available");
            return postData;
        }).toList();
        response.put("status", "success");
        response.put("savedJobs", posts);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
