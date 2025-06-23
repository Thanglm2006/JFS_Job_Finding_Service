package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.DTO.Schedule;
import com.example.JFS_Job_Finding_Service.repository.*;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public ResponseEntity<?> applyForJob(String token, String jobId, String position) {
        if(!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("You do not have permission to apply for jobs");
        }
        if(position!= null && position.isEmpty()|| position.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Position cannot be empty");
        }
        String finalPosition = position.replace("_"," ");
        Applicant applicant = jwtUtil.getApplicant(token);
        JobPost job= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));
        Application application = new Application(job, applicant,finalPosition);
        application.setAppliedAt(Instant.now());
        applicationRepository.save(application);
        Notification notification = new Notification();
        notification.setUser(job.getEmployer().getUser());
        notification.setMessage("New application received for job ID " + jobId + " from applicant " + applicant.getUser().getFullName());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application submitted successfully for job ID: " + jobId);
    }
    public ResponseEntity<?> unApplyForJob(String token, String jobId) {
        if(!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("You do not have permission to unapply for jobs");
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));
        Application application = applicationRepository.findByJobAndApplicant(job, applicant)
                .orElseThrow(() -> new RuntimeException("Application not found for job ID: " + jobId));
        applicationRepository.delete(application);
        Notification notification = new Notification();
        notification.setUser(job.getEmployer().getUser());
        notification.setMessage("Application for job ID " + jobId + " has been withdrawn by applicant " + applicant.getUser().getFullName());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application withdrawn successfully for job ID: " + jobId);
    }
    public ResponseEntity<?> accept(String token, String jobId, Long applicantId) {
        if(!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("You do not have permission to accept applications");
        }

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        JobPost job= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));
        Application application = applicationRepository.findByJobAndApplicant(job, applicant)
                .orElseThrow(() -> new RuntimeException("Application not found for job ID: " + jobId));
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
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application accepted successfully for job ID: " + job.getId());
    }
    public ResponseEntity<?> reject(String token, String jobId, Long applicantId) {
        if(!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("You do not have permission to reject applications");
        }

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        JobPost job= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));
        Application application = applicationRepository.findByJobAndApplicant(job, applicant)
                .orElseThrow(() -> new RuntimeException("Application not found for job ID: " + jobId));
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
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application rejected for applicant " + applicant.getUser().getFullName());
    }
    public ResponseEntity<?> setSchedule(String token,String applicantId, String jobId, List<Schedule> schedules) {
        if(!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("You do not have permission to set schedules");
        }
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));
        List<com.example.JFS_Job_Finding_Service.models.Schedule> existingSchedules = scheduleRepository.findByApplicant(applicant);
        List<com.example.JFS_Job_Finding_Service.models.Schedule> schedulesForJob = scheduleRepository.findByJob(job);
        if (schedulesForJob != null && !schedulesForJob.isEmpty()) {
            scheduleRepository.deleteAll(existingSchedules);
        }
        scheduleRepository.deleteAll(existingSchedules);
        for( Schedule schedule : schedules) {
            if (schedule.getStartTime()>=(schedule.getEndTime())) {
                return ResponseEntity.badRequest().body("Start time cannot be after end time");
            }
            if(schedule.getDay()!=null && !schedule.getDay().matches("^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)$")) {
                return ResponseEntity.badRequest().body("Invalid day of the week: " + schedule.getDay());
            }
            for( com.example.JFS_Job_Finding_Service.models.Schedule existingSchedule : existingSchedules) {
                if (existingSchedule.getDay().equals(schedule.getDay()) &&
                        ((schedule.getStartTime() >= existingSchedule.getStartTime() && schedule.getStartTime() <= existingSchedule.getEndTime()) ||
                                (schedule.getEndTime() >= existingSchedule.getStartTime() && schedule.getEndTime() <= existingSchedule.getEndTime()))) {
                    return ResponseEntity.badRequest().body("Schedule conflicts with existing schedule on " + schedule.getDay());
                }
            }
            try{
                com.example.JFS_Job_Finding_Service.models.Schedule scheduleModel = new com.example.JFS_Job_Finding_Service.models.Schedule();
                scheduleModel.setApplicant(applicant);
                scheduleModel.setJob(job);
                scheduleModel.setStartTime(schedule.getStartTime());
                scheduleModel.setEndTime(schedule.getEndTime());
                scheduleModel.setDay(schedule.getDay());
                scheduleModel.setDescription(schedule.getDescription());
                scheduleRepository.save(scheduleModel);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Schedule could not be saved, invalid schedule data");
            }
        }
        return ResponseEntity.status(200).body("Application Schedule saved");
    }
    public ResponseEntity<?> getAllApplicationForEmployer(String token){
        if(!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("You do not have permission to view applications");
        }
        Employer employer = jwtUtil.getEmployer(token);
        if (employer == null) {
            return ResponseEntity.status(404).body("Employer not found");
        }
        List<JobPost> jobPosts = jobPostRepository.findByEmployer(employer);
        Map<String, Object> response = new HashMap<>();
        if (jobPosts.isEmpty()) {
            response.put("status", "fail");
            response.put("message", "No job posts found for this employer");
            return ResponseEntity.ok(response);
        }
        List<Map<String, Object>> applications = jobPosts.stream().map(jobPost -> {
            Application application = applicationRepository.findByJob(jobPost);
            if (application == null) {
                return null;
            }
            Applicant applicant = application.getApplicant();
                    String employerName = jobPost.getEmployer() != null ? jobPost.getEmployer().getFullName() : "Unknown";
                    List<ImageFolders> folder = List.of();
                    List<String> pics = new ArrayList<>(List.of());
                    if (jobPost.getWorkspacePicture() != null) {
                        folder = imageFoldersRepository.findByFolderName(jobPost.getWorkspacePicture());
                    }
                    for( ImageFolders imageFolder : folder){
                        if(imageFolder.getFolderName().equals(jobPost.getWorkspacePicture())){
                            pics.add(imageFolder.getFileName());
                        }
                    }
                    boolean isSaved=false;
                    boolean isApplied=true;
                    if(applicant!=null)isSaved= savedJobRepository.findByApplicantAndJob(applicant, jobPost) != null;
                    int totalSaved = savedJobRepository.countByJob(jobPost);            Map<String, Object> applicationData = new HashMap<>();
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
            applicationData.put("applicationId", application.getId());
            applicationData.put("applicantName", application.getApplicant().getUser().getFullName());
            applicationData.put("status", application.getStatus());
            applicationData.put("resume", application.getApplicant().getResume());
            applicationData.put("position", application.getPosition());
            applicationData.put("appliedAt", application.getAppliedAt());
            applicationData.put("applicantId", application.getApplicant().getId());
            applicationData.put("userId", application.getApplicant().getUser().getId());
            applicationData.put("jobPost",postData);
            applicationData.put("avatar", application.getApplicant().getUser().getAvatarUrl());

            return applicationData;
        }).filter(Objects::nonNull)
                .toList();
        response.put("status", "success");
        response.put("applications", applications);
        return ResponseEntity.ok(response);
    }
}
