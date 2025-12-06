package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Application;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.models.Notification;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import com.example.JFS_Job_Finding_Service.repository.ApplicationRepository;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    private TokenService tokenService;

    public ResponseEntity<?> applyForJob(String token, Long jobId) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("You do not have permission to apply for jobs");
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        JobPost job= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));
        Application application = new Application(job, applicant);
        application.setAppliedAt(java.time.Instant.now());
        applicationRepository.save(application);
        Notification notification = new Notification();
        notification.setUser(job.getEmployer().getUser());
        notification.setMessage("New application received for job ID " + jobId + " from applicant " + applicant.getUser().getFullName());
        notification.setRead(false);
        notification.setCreatedAt(java.time.Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application submitted successfully for job ID: " + jobId);
    }
    public ResponseEntity<?> accept(String token, Long applicationId, Long applicantId) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("You do not have permission to accept applications");
        }

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        Application application = applicationRepository.findByApplicant(applicant)
                .orElseThrow(() -> new RuntimeException("Application not found with ID: " + applicationId));
        JobPost job = application.getJob();
        if (!job.getEmployer().getUser().getEmail().equals(jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(403).body("You do not own this job post");
        }
        if (application.getStatus().equals("Accepted")) {
            return ResponseEntity.status(400).body("Application already accepted");
        }
        application.setStatus("Accepted");
        applicationRepository.save(application);
        Notification notification = new Notification();
        notification.setUser(applicant.getUser());
        notification.setMessage("Your application for job ID " + job.getId() + " has been accepted.");
        notification.setRead(false);
        notification.setCreatedAt(java.time.Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application accepted successfully for job ID: " + job.getId());
    }
    public ResponseEntity<?> reject(String token, Long applicationId, Long applicantId) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("You do not have permission to reject applications");
        }

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        Application application = applicationRepository.findByApplicant(applicant)
                .orElseThrow(() -> new RuntimeException("Application not found with ID: " + applicationId));
        JobPost job = application.getJob();
        if (!job.getEmployer().getUser().getEmail().equals(jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(403).body("You do not own this job post");
        }
        if (application.getStatus().equals("Rejected")) {
            return ResponseEntity.status(400).body("Application already rejected");
        }
        application.setStatus("Rejected");
        applicationRepository.save(application);
        Notification notification = new Notification();
        notification.setUser(applicant.getUser());
        notification.setMessage("Your application for job ID " + job.getId() + " has been rejected.");
        notification.setRead(false);
        notification.setCreatedAt(java.time.Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application rejected for applicant " + applicant.getUser().getFullName());
    }
}
