package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.DTO.Schedule;
import com.example.JFS_Job_Finding_Service.models.Enum.ApplicationStatus;
import com.example.JFS_Job_Finding_Service.repository.*;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ApplicationService {
    @Autowired
    JobPostRepository jobPostRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private SavedJobRepository savedJobRepository;
    @Autowired
    private ImageFoldersRepository imageFoldersRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private TokenService tokenService;

    public ResponseEntity<?> applyForJob(String token, String jobId, String position, String cv) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép. Vui lòng đăng nhập lại.");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền thực hiện thao tác ứng tuyển.");
        }
        if(position!= null && position.isEmpty()|| position.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vị trí ứng tuyển không được để trống.");
        }
        String finalPosition = position.replace("_"," ");
        Applicant applicant = jwtUtil.getApplicant(token);
        JobPost job= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + jobId));
        Application application = new Application(job, applicant,finalPosition,cv);
        application.setAppliedAt(Instant.now());
        applicationRepository.save(application);
        Notification notification = new Notification();
        notification.setUser(job.getEmployer().getUser());
        notification.setMessage(applicant.getUser().getFullName() + " đã nộp đơn ứng tuyển cho vị trí " + job.getTitle());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Đã nộp đơn ứng tuyển thành công cho công việc: " + job.getTitle());
    }
    public ResponseEntity<?> unApplyForJob(String token, String jobId) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền rút đơn ứng tuyển.");
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + jobId));
        Application application = applicationRepository.findByJobAndApplicant(job, applicant)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển của bạn cho công việc này."));
        applicationRepository.delete(application);
        Notification notification = new Notification();
        notification.setUser(job.getEmployer().getUser());
        notification.setMessage("Đơn ứng tuyển cho công việc " + job.getTitle() + " đã được rút bởi ứng viên " + applicant.getUser().getFullName());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Đã rút đơn ứng tuyển thành công.");
    }
    public ResponseEntity<?> accept(String token, String jobId, String applicantId, LocalDateTime interview) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền phê duyệt đơn ứng tuyển.");
        }

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên với ID: " + applicantId));
        JobPost job= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + jobId));
        Application application = applicationRepository.findByJobAndApplicant(job, applicant)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển tương ứng."));
        if (!job.getEmployer().getUser().getEmail().equals(jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(403).body("Bạn không có quyền quản lý bài đăng tuyển dụng này.");
        }
        if (application.getStatus().equals(ApplicationStatus.ACCEPTED)) {
            return ResponseEntity.status(400).body("Đơn ứng tuyển này đã được chấp nhận từ trước.");
        }
        application.setStatus(ApplicationStatus.ACCEPTED);
        application.setAppliedAt(Instant.now());
        applicationRepository.save(application);
        Notification notification = new Notification();
        notification.setUser(applicant.getUser());
        notification.setMessage("Chúc mừng! Bạn đã được nhận vào làm việc tại vị trí " + application.getPosition() + " cho " + job.getEmployer().getOrgName());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Đã phê duyệt đơn ứng tuyển thành công.");
    }
    public ResponseEntity<?> reject(String token, String jobId, String applicantId, String reason) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền từ chối đơn ứng tuyển.");
        }

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên với ID: " + applicantId));
        JobPost job= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + jobId));
        Application application = applicationRepository.findByJobAndApplicant(job, applicant)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển tương ứng."));
        if (!job.getEmployer().getUser().getEmail().equals(jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(403).body("Bạn không có quyền quản lý bài đăng tuyển dụng này.");
        }
        if (application.getStatus().equals(ApplicationStatus.REJECTED) ){
            return ResponseEntity.status(400).body("Đơn ứng tuyển này đã bị từ chối trước đó.");
        }
        application.setStatus(ApplicationStatus.REJECTED);
        application.setReason(reason);
        applicationRepository.save(application);
        Notification notification = new Notification();
        notification.setUser(applicant.getUser());
        notification.setMessage("Rất tiếc, đơn ứng tuyển của bạn tại " + job.getEmployer().getOrgName() + " cho vị trí " + application.getPosition() + " đã bị từ chối.");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Đã từ chối đơn ứng tuyển của ứng viên " + applicant.getUser().getFullName());
    }

    public ResponseEntity<?> getAllApplicationForEmployer(String token){
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền xem danh sách ứng tuyển.");
        }
        Employer employer = jwtUtil.getEmployer(token);
        if (employer == null) {
            return ResponseEntity.status(404).body("Không tìm thấy thông tin nhà tuyển dụng.");
        }
        List<JobPost> jobPosts = jobPostRepository.findByEmployer(employer);
        Map<String, Object> response = new HashMap<>();
        if (jobPosts.isEmpty()) {
            response.put("status", "fail");
            response.put("message", "Hiện tại bạn chưa có bài đăng tuyển dụng nào.");
            return ResponseEntity.ok(response);
        }
        List<Map<String, Object>> applications = jobPosts.stream().flatMap(jobPost -> {
            List<Application> applicationsL = applicationRepository.findByJob(jobPost);
            List<Map<String, Object>> applicationDataList = new ArrayList<>();
            for(Application application : applicationsL) {
                if (application == null||application.getStatus().equals(ApplicationStatus.REJECTED)|| application.getStatus().equals(ApplicationStatus.ACCEPTED)) {
                    continue;
                }
                Applicant applicant = application.getApplicant();
                String employerName = jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown";
                List<ImageFolders> folder = List.of();
                List<String> pics = new ArrayList<>(List.of());
                if (jobPost.getWorkspacePicture() != null) {
                    folder = imageFoldersRepository.findByFolderName(jobPost.getWorkspacePicture());
                }
                for (ImageFolders imageFolder : folder) {
                    if (imageFolder.getFolderName().equals(jobPost.getWorkspacePicture())) {
                        pics.add(imageFolder.getFileName());
                    }
                }
                boolean isSaved = false;
                boolean isApplied = true;
                if (applicant != null) isSaved = !savedJobRepository.findByApplicantAndJob(applicant, jobPost).isEmpty();
                int totalSaved = savedJobRepository.countByJob(jobPost);
                Map<String, Object> applicationData = new HashMap<>();
                Map<String, Object> postData = new HashMap<>();
                postData.put("id", jobPost.getId());
                postData.put("title", jobPost.getTitle());
                postData.put("employerName", employerName);
                postData.put("userId", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getId() : null);
                postData.put("isSaved", isSaved);
                postData.put("employerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
                postData.put("description", jobPost.getJobDescription());
                postData.put("avatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
                postData.put("workspacePicture", pics.toArray());
                postData.put("createdAt", jobPost.getCreatedAt());
                postData.put("totalSaved", totalSaved);
                postData.put("isApplied", isApplied);
                applicationData.put("applicationId", application.getId());
                applicationData.put("applicantName", application.getApplicant().getUser().getFullName());
                applicationData.put("status", application.getStatus());
                applicationData.put("resume", application.getApplicant().getResume());
                applicationData.put("position", application.getPosition());
                applicationData.put("appliedAt", application.getAppliedAt());
                applicationData.put("applicantId", application.getApplicant().getId());
                applicationData.put("userId", application.getApplicant().getUser().getId());
                applicationData.put("jobPost", postData);
                applicationData.put("avatar", application.getApplicant().getUser().getAvatarUrl());

                applicationDataList.add(applicationData);
            }
            return applicationDataList.stream();
        }).filter(Objects::nonNull).toList();
        response.put("status", "success");
        response.put("applications", applications);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getJobs(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền truy cập vào danh sách này.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            response.put("status", "fail");
            response.put("message", "Tài khoản của bạn không phải là ứng viên.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<Application> applicationPage = applicationRepository.findByApplicant(applicant, pageable);
        List<Map<String, Object>> applications = applicationPage.getContent().stream().map(application -> {
            if(!application.getStatus().equals(ApplicationStatus.ACCEPTED)) {
                return null;
            }
            JobPost jobPost = application.getJob();
            Map<String, Object> applicationData = new HashMap<>();
            applicationData.put("applicationId", application.getId());
            applicationData.put("jobId", jobPost.getId());
            applicationData.put("jobTitle", jobPost.getTitle());
            applicationData.put("jobDescription", jobPost.getJobDescription());
            applicationData.put("jobEmployerId", jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null);
            applicationData.put("jobEmployerName", jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown");
            applicationData.put("employerAvatar", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getAvatarUrl() : null);
            applicationData.put("job",application.getPosition());
            applicationData.put("acceptedAt", application.getAppliedAt());
            applicationData.put("applicantUserId", application.getApplicant().getUser().getId());
            applicationData.put("employerUserId", jobPost.getEmployer() != null ? jobPost.getEmployer().getUser().getId() : null);
            return applicationData;
        }).filter(Objects::nonNull).toList();
        response.put("status", "success");
        response.put("jobs", applications);
        response.put("totalPages", applicationPage.getTotalPages());
        response.put("currentPage", applicationPage.getNumber());
        response.put("totalApplications", applicationPage.getTotalElements());
        response.put("totalApplicationsCount", applicationPage.getTotalElements());
        response.put("pageSize", applicationPage.getSize());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}