package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.*;
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


import java.util.*;

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
    @Autowired
    private EmployerRepository employerRepository;
    @Autowired
    private NotificationRepository notificationRepository;
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
            {
                try {
                    String employerName = jobPost.getEmployer() != null ? jobPost.getEmployer().getFullName() : "Unknown";
                    List<ImageFolders> folder = List.of();
                    List<String> pics = new java.util.ArrayList<>(List.of());
                    if (jobPost.getWorkspacePicture() != null) {
                        folder = imageFoldersRepository.findByFolderName(jobPost.getWorkspacePicture());
                    }
                    for (ImageFolders imageFolder : folder) {
                        if (imageFolder.getFolderName().equals(jobPost.getWorkspacePicture())) {
                            pics.add(imageFolder.getFileName());
                        }
                    }
                    boolean isSaved = false;
                    boolean isApplied = false;
                    if (applicant != null)
                        isSaved = !savedJobRepository.findByApplicantAndJob(applicant, jobPost).isEmpty();
                    int totalSaved = savedJobRepository.countByJob(jobPost);
                    if (applicant != null) isApplied = applicationRepository.findByApplicant(applicant)
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
                } catch (Exception e) {
                    System.err.println("⚠️ Error with jobPost ID: " + jobPost.getId());
                    e.printStackTrace();
                    throw new RuntimeException("JobPost " + jobPost.getId(), e);
                }
            }
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng thành công");
        response.put("posts", posts);
        response.put("totalPages", jobPostsPage.getTotalPages());
        response.put("currentPage", jobPostsPage.getNumber());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> fullTextSearchPosts(String token, String pattern) {
        Map<String, Object> response = new HashMap<>();

        if (!jwtUtil.validateToken(token)) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không phải là ứng viên");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        List<JobPost> jobPosts = jobPostRepository.searchWithPGroonga(pattern); // returns max 10

        List<Map<String, Object>> posts = jobPosts.stream().map(jobPost -> {
            try {
                String employerName = jobPost.getEmployer() != null ? jobPost.getEmployer().getFullName() : "Unknown";
                List<ImageFolders> folder = List.of();
                List<String> pics = new ArrayList<>();

                if (jobPost.getWorkspacePicture() != null) {
                    folder = imageFoldersRepository.findByFolderName(jobPost.getWorkspacePicture());
                }

                for (ImageFolders imageFolder : folder) {
                    if (imageFolder.getFolderName().equals(jobPost.getWorkspacePicture())) {
                        pics.add(imageFolder.getFileName());
                    }
                }

                boolean isSaved = false;
                boolean isApplied = false;

                if (applicant != null)
                    isSaved = !savedJobRepository.findByApplicantAndJob(applicant, jobPost).isEmpty();

                int totalSaved = savedJobRepository.countByJob(jobPost);

                if (applicant != null)
                    isApplied = applicationRepository.findByApplicant(applicant).stream()
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
            } catch (Exception e) {
                System.err.println("⚠️ Error with jobPost ID: " + jobPost.getId());
                e.printStackTrace();
                throw new RuntimeException("JobPost " + jobPost.getId(), e);
            }
        }).toList();

        response.put("status", "success");
        response.put("message", "Kết quả tìm kiếm");
        response.put("posts", posts);
        response.put("totalResults", posts.size());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    public ResponseEntity<?> getSomePostOfEmployer(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();
        if (!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Employer employerOptional = jwtUtil.getEmployer(token);
        if (employerOptional == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không phải là nhà tuyển dụng");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        Page<JobPost> jobPostsPage = jobPostRepository.findByEmployer(employerOptional, pageable);
        List<Map<String, Object>> posts = jobPostsPage.getContent().stream().map(jobPost -> {
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
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("employerName", employerOptional.getFullName());
            postData.put("userId", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getId() : null);
            postData.put("isSaved", true);
            postData.put("employerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            postData.put("description", jobPost.getDescription());
            postData.put("avatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
            postData.put("workspacePicture", pics.toArray());
            postData.put("createdAt", jobPost.getCreatedAt());
            postData.put("totalSaved", savedJobRepository.countByJob(jobPost));
            postData.put("isApplied", false);
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
        Notification notification=new Notification();
        notification.setUser(jobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng với tiêu đề \"" + jobPost.getTitle() + "\" đã bị xóa.");
        notification.setRead(false);
        notificationRepository.save(notification);
        response.put("status", "success");
        response.put("message", "Xóa bài đăng thành công");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    public ResponseEntity<?> getAppliedJobs(String token, int page, int size) {
    Map<String, Object> response = new HashMap<>();
        if (!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không phải là ứng viên");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Application> applicationPage = applicationRepository.findByApplicant(applicant, pageable);
        List<Map<String, Object>> applications = applicationPage.getContent().stream().map(application -> {
            JobPost jobPost = application.getJob();
            Map<String, Object> applicationData = new HashMap<>();
            applicationData.put("applicationId", application.getId());
            applicationData.put("jobId", jobPost.getId());
            applicationData.put("jobTitle", jobPost.getTitle());
            applicationData.put("jobDescription", jobPost.getDescription());
            applicationData.put("jobCreatedAt", jobPost.getCreatedAt());
            applicationData.put("applicantId", applicant.getId());
            applicationData.put("workspacePicture", jobPost.getWorkspacePicture());
            applicationData.put("jobEmployerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            applicationData.put("jobEmployerName", jobPost.getEmployer() != null ? jobPost.getEmployer().getFullName() : "Unknown");
            applicationData.put("status", application.getStatus());
            applicationData.put("appliedAt", application.getAppliedAt());
            applicationData.put("applicantName", applicant.getUser().getFullName());
            applicationData.put("employerAvatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
            return applicationData;
        }).toList();
        response.put("status", "success");
        response.put("applications", applications);
        response.put("totalPages", applicationPage.getTotalPages());
        response.put("currentPage", applicationPage.getNumber());
        response.put("totalApplications", applicationPage.getTotalElements());
        response.put("totalApplicationsCount", applicationPage.getTotalElements());
        response.put("pageSize", applicationPage.getSize());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
