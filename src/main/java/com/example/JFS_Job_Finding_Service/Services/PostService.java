package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.PostingRequest;
import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.ImageFolders;
import com.example.JFS_Job_Finding_Service.models.JobPost;
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


    public ResponseEntity<?> getSomePosts(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();

        if (!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Applicant applicant= jwtUtil.getApplicant(token);
        if (applicant == null) {
            System.out.println("Applicant not found");
            response.put("status", "fail");
            response.put("message", "Người dùng không tồn tại");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
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
            boolean isSaved= savedJobRepository.findByApplicantAndJob(applicant, jobPost) != null;
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("employerName", employerName);
            postData.put("isSaved", isSaved);
            postData.put("employerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            postData.put("description", jobPost.getDescription());
            postData.put("workspacePicture", pics.toArray());
            postData.put("createdAt", jobPost.getCreatedAt());
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng thành công");
        response.put("posts", posts);
        response.put("totalPages", jobPostsPage.getTotalPages());
        response.put("currentPage", jobPostsPage.getNumber());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
