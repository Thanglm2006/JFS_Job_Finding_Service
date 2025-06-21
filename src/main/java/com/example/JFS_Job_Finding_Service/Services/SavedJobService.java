package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.ImageFolders;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.models.SavedJob;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import com.example.JFS_Job_Finding_Service.repository.ImageFoldersRepository;
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
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ImageFoldersRepository imageFoldersRepository;


    public ResponseEntity<?> saveJob(String token, String jobId) {

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
        System.out.println("Job saved successfully for applicant: " + applicant.getId() + " and job: " + jobPost.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    public ResponseEntity<?> unSaveJob(String token, String jobId) {
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
        JobPost job= jobPostRepository.findById(jobId).orElse(null);
        if (job == null) {
            response.put("status", "fail");
            response.put("message", "Job not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        SavedJob savedJob = savedJobRepository.findByApplicantAndJob(applicant, job);
        if (savedJob == null) {
            response.put("status", "fail");
            response.put("message", "Saved job not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        savedJobRepository.delete(savedJob);
        response.put("status", "success");
        response.put("message", "Job unsaved successfully");
        System.out.println("Job unsaved successfully for applicant: " + applicant.getId() + " and job: " + job.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
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
            List<ImageFolders> folder = List.of();
            List<String> pics = new java.util.ArrayList<>(List.of());
            if (savedJob.getJob().getWorkspacePicture() != null) {
                folder = imageFoldersRepository.findByFolderName(savedJob.getJob().getWorkspacePicture());
            }
            for( ImageFolders imageFolder : folder){
                if(imageFolder.getFolderName().equals(savedJob.getJob().getWorkspacePicture())){
                    pics.add(imageFolder.getFileName());
                }
            }
            JobPost jobPost = savedJob.getJob();
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("description", jobPost.getDescription());
            postData.put("createdAt", jobPost.getCreatedAt());
            postData.put("savedAt", savedJob.getSavedAt());
            postData.put("isSaved", true);
            postData.put("avatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
            postData.put("employerName", jobPost.getEmployer() != null ? jobPost.getEmployer().getFullName() : "Unknown");
            postData.put("employerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            postData.put("workspacePicture", pics.toArray());
            return postData;
        }).toList();
        response.put("status", "success");
        response.put("savedJobs", posts);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
