package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.PostingRequest;
import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.ImageFolders;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.repository.*;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostService {
    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ImageFoldersRepository imageFoldersRepository;
    @Autowired
    private SavedJobRepository savedJobRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ApplicationRepository applicationRepository;


    public ResponseEntity<?> getSomePosts(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();

        if (!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Applicant applicant= jwtUtil.getApplicant(token);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobPost> jobPostsPage = jobPostRepository.findAll(pageable);

        List<Map<String, Object>> posts = jobPostsPage.getContent().stream().map(jobPost -> {
            String employerName = jobPost.getEmployer() != null ? jobPost.getEmployer().getFullName() : "Unknown";
            List<ImageFolders> folder = List.of();
            List<String> pics = new java.util.ArrayList<>(List.of());
            if (jobPost.getWorkspacePicture() != null) {
                folder = imageFoldersRepository.findByFolderName(jobPost.getWorkspacePicture());
            }
            for( ImageFolders imageFolder : folder){
                if(imageFolder.getFolderName().equals(jobPost.getWorkspacePicture())){
                    pics.add(imageFolder.getFileName());
                }
            }
            boolean isSaved=false;
            boolean isApplied=false;
            if(applicant!=null)isSaved= savedJobRepository.findByApplicantAndJob(applicant, jobPost) != null;
            int totalSaved = savedJobRepository.countByJob(jobPost);
            if(applicant!=null)isApplied= applicationRepository.findByApplicant(applicant)
                    .stream()
                    .anyMatch(application -> application.getJob().getId().equals(jobPost.getId()));
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("employerName", employerName);
            postData.put("userId", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getId() : null);
            postData.put("isSaved", isSaved);
            postData.put("employerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            postData.put("description", jobPost.getDescription());
            postData.put("avatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
            postData.put("workspacePicture", pics.toArray());
            postData.put("createdAt", jobPost.getCreatedAt());
            postData.put("totalSaved", totalSaved);
            postData.put("isApplied", isApplied);
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng thành công");
        response.put("posts", posts);
        response.put("totalPages", jobPostsPage.getTotalPages());
        response.put("currentPage", jobPostsPage.getNumber());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    public ResponseEntity<?> deletePost(String token, String postId) {
        Map<String, Object> response = new HashMap<>();
        if (!jwtUtil.checkPermission(token, "Admin")&&
                !jwtUtil.checkPermission(token, "Employer")) {
            System.out.println("Unauthorized access attempt with token: " + token);
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        JobPost jobPost = jobPostRepository.findById(postId).orElse(null);
        if (jobPost == null) {
            response.put("status", "fail");
            response.put("message", "Bài đăng không tồn tại");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        jobPostRepository.delete(jobPost);
        savedJobRepository.deleteAll(savedJobRepository.findByJob(jobPost));
        response.put("status", "success");
        response.put("message", "Xóa bài đăng thành công");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
