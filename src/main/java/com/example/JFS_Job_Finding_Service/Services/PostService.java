package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Post.JobSearchRequest;
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

import java.math.BigDecimal;
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
    private NotificationRepository notificationRepository;
    @Autowired
    private TokenService tokenService;

    private String formatSalary(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return min.toPlainString() + " - " + max.toPlainString();
        }
        return "Thương lượng";
    }

    public ResponseEntity<?> getSomePosts(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền truy cập chức năng này.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobPost> jobPostsPage = jobPostRepository.findAll(pageable);

        // Map to simplified structure
        List<Map<String, Object>> posts = jobPostsPage.getContent().stream().map(jobPost -> {
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("orgName", jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown");
            postData.put("salary", formatSalary(jobPost.getSalaryMin(), jobPost.getSalaryMax()));
            postData.put("type", jobPost.getType());
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng thành công.");
        response.put("posts", posts);
        response.put("totalPages", jobPostsPage.getTotalPages());
        response.put("currentPage", jobPostsPage.getNumber());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Updated to use DTO and single address search
    public ResponseEntity<?> fullTextSearchPosts(String token, JobSearchRequest searchDTO) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token)) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền truy cập.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            response.put("status", "fail");
            response.put("message", "Tài khoản của bạn không phải là ứng viên.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        if (searchDTO.getType() != null && searchDTO.getType().isEmpty()) {
            searchDTO.setType(null);
        }
        List<JobPost> jobPosts = jobPostRepository.searchWithPGroonga(
                searchDTO.getKeyword(),
                searchDTO.getType(),
                searchDTO.getAddress(),
                searchDTO.getMinSalary(),
                searchDTO.getMaxSalary(),
                searchDTO.getLimit(),
                searchDTO.getOffset()
        );

        List<Map<String, Object>> posts = jobPosts.stream().map(jobPost -> {
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("orgName", jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown");
            postData.put("salary", formatSalary(jobPost.getSalaryMin(), jobPost.getSalaryMax()));
            postData.put("type", jobPost.getType());
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Kết quả tìm kiếm phù hợp.");
        response.put("posts", posts);
        response.put("totalResults", posts.size());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> getSomePostOfEmployer(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền truy cập.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Employer employer = jwtUtil.getEmployer(token);
        if (employer == null) {
            response.put("status", "fail");
            response.put("message", "Tài khoản của bạn không phải là nhà tuyển dụng.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobPost> jobPostsPage = jobPostRepository.findByEmployer(employer, pageable);

        List<Map<String, Object>> posts = jobPostsPage.getContent().stream().map(jobPost -> {
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("orgName", employer.getOrgName());
            postData.put("salary", formatSalary(jobPost.getSalaryMin(), jobPost.getSalaryMax()));
            postData.put("type", jobPost.getType());
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng thành công.");
        response.put("posts", posts);
        response.put("totalPages", jobPostsPage.getTotalPages());
        response.put("currentPage", jobPostsPage.getNumber());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> getJobPostDetail(String token, String postId) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return new ResponseEntity<>(Map.of("status", "fail", "message", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        JobPost jobPost = jobPostRepository.findById(postId).orElse(null);
        if (jobPost == null) {
            response.put("status", "fail");
            response.put("message", "Bài đăng không tồn tại.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Applicant applicant = jwtUtil.getApplicant(token);

        try {
            String employerName = jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown";

            // Image handling
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
            if (applicant != null) {
                isSaved = !savedJobRepository.findByApplicantAndJob(applicant, jobPost).isEmpty();
                isApplied = applicationRepository.findByApplicant(applicant)
                        .stream()
                        .anyMatch(application -> application.getJob().getId().equals(jobPost.getId()));
            }
            int totalSaved = savedJobRepository.countByJob(jobPost);

            Map<String, Object> postData = new HashMap<>();
            postData.put("id", jobPost.getId());
            postData.put("title", jobPost.getTitle());
            postData.put("employerName", employerName);
            postData.put("userId", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getId() : null);
            postData.put("isSaved", isSaved);
            postData.put("employerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            postData.put("description", jobPost.getJobDescription());
            postData.put("requirements", jobPost.getRequirements());
            postData.put("responsibilities", jobPost.getResponsibilities());
            postData.put("advantages", jobPost.getAdvantages());
            postData.put("extension", jobPost.getExtension());
            postData.put("type", jobPost.getType());
            postData.put("addresses", jobPost.getAddresses());
            postData.put("positions", jobPost.getPositions());
            postData.put("salaryMin", jobPost.getSalaryMin());
            postData.put("salaryMax", jobPost.getSalaryMax());
            postData.put("avatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
            postData.put("workspacePicture", pics.toArray());
            postData.put("createdAt", jobPost.getCreatedAt());
            postData.put("totalSaved", totalSaved);
            postData.put("isApplied", isApplied);

            response.put("status", "success");
            response.put("post", postData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("status", "error", "message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> deletePost(String token, String postId) {
        Map<String, Object> response = new HashMap<>();
        if (!jwtUtil.checkPermission(token, "Admin") &&
                !jwtUtil.checkPermission(token, "Employer")) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền xóa bài đăng này.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        JobPost jobPost = jobPostRepository.findById(postId).orElse(null);
        if (jobPost == null) {
            response.put("status", "fail");
            response.put("message", "Bài đăng không tồn tại.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        jobPostRepository.delete(jobPost);
        savedJobRepository.deleteAll(savedJobRepository.findByJob(jobPost));
        Notification notification = new Notification();
        notification.setUser(jobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng với tiêu đề \"" + jobPost.getTitle() + "\" đã được xóa khỏi hệ thống.");
        notification.setRead(false);
        notificationRepository.save(notification);
        response.put("status", "success");
        response.put("message", "Đã xóa bài đăng thành công.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> getAppliedJobs(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền truy cập.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            response.put("status", "fail");
            response.put("message", "Tài khoản của bạn không phải là ứng viên.");
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
            // Shortened info for list view
            applicationData.put("jobCreatedAt", jobPost.getCreatedAt());
            applicationData.put("applicantId", applicant.getId());
            applicationData.put("jobEmployerName", jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown");
            applicationData.put("status", application.getStatus());
            applicationData.put("appliedAt", application.getAppliedAt());
            applicationData.put("employerAvatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
            return applicationData;
        }).toList();
        response.put("status", "success");
        response.put("applications", applications);
        response.put("totalPages", applicationPage.getTotalPages());
        response.put("currentPage", applicationPage.getNumber());
        response.put("totalApplications", applicationPage.getTotalElements());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}