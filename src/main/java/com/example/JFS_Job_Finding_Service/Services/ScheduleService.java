package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Schedule;
import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.models.Notification;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import com.example.JFS_Job_Finding_Service.repository.ScheduleRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenService tokenService;

    public ResponseEntity<?> setSchedule(String token, String applicantId, String jobId, List<Schedule> schedules) {
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("You do not have permission to set schedules");
        }
        //check if intersection in schedules
        if (schedules == null || schedules.isEmpty()) {
            return ResponseEntity.badRequest().body("Schedules cannot be empty");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getStartTime() >= schedule.getEndTime())) {
            return ResponseEntity.badRequest().body("Start time cannot be after end time");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getDay() == null || !schedule.getDay().matches("^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)$"))) {
            return ResponseEntity.badRequest().body("Invalid day of the week in schedules");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getDescription() == null || schedule.getDescription().isEmpty())) {
            return ResponseEntity.badRequest().body("Schedule description cannot be empty");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getStartTime() < 0 || schedule.getEndTime() < 0)) {
            return ResponseEntity.badRequest().body("Start time and end time must be non-negative");
        }
        for (int i = 0; i < schedules.size(); i++) {
            Schedule s1 = schedules.get(i);
            for (int j = i + 1; j < schedules.size(); j++) {
                Schedule s2 = schedules.get(j);
                if (s1.getDay().equals(s2.getDay()) &&
                        s1.getStartTime() <= s2.getEndTime() &&
                        s2.getStartTime() < s1.getEndTime()) {
                    return ResponseEntity.badRequest().body("Schedules conflict on " + s1.getDay() + " between " + s1.getStartTime() + " and " + s1.getEndTime());
                }
            }
        }
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));
        List<com.example.JFS_Job_Finding_Service.models.Schedule> schedulesForJob = scheduleRepository.findByJob(job);
        if (schedulesForJob != null && !schedulesForJob.isEmpty()) {
            scheduleRepository.deleteAll(schedulesForJob);
        }
        List<com.example.JFS_Job_Finding_Service.models.Schedule> existingSchedules = scheduleRepository.findByApplicant(applicant);

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
                    System.out.println("Schedule conflicts with existing schedule on " + existingSchedule.getDay() + " from " + existingSchedule.getStartTime() + " to " + existingSchedule.getEndTime() + "and another shift" + schedule.getStartTime() + " to " + schedule.getEndTime());
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
                Notification notification = new Notification();
                notification.setUser(applicant.getUser());
                notification.setMessage("Lịch làm việc đã được chỉnh sửa" + job.getTitle() + " bởi " + job.getEmployer().getUser().getFullName());
                notification.setRead(false);
                notification.setCreatedAt(Instant.now());
                scheduleRepository.save(scheduleModel);
                notificationRepository.save(notification);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Schedule could not be saved, invalid schedule data");
            }
        }
        return ResponseEntity.status(200).body("Application Schedule saved");
    }
    public ResponseEntity<?> getSchedulesForApplicant(String token){
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Unauthorized access");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("You do not have permission to view schedules");
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            return ResponseEntity.status(404).body("Applicant not found");
        }
        List<com.example.JFS_Job_Finding_Service.models.Schedule> schedules = scheduleRepository.findByApplicant(applicant);
        schedules.sort(Comparator.comparingInt(com.example.JFS_Job_Finding_Service.models.Schedule::getStartTime));
        if (schedules.isEmpty()) {
            return ResponseEntity.ok("No schedules found for this applicant");
        }
        Map<String, List<com.example.JFS_Job_Finding_Service.models.Schedule>> schedulesByDay = new HashMap<>();
        for (com.example.JFS_Job_Finding_Service.models.Schedule schedule : schedules) {
            String day = schedule.getDay();
            if (!schedulesByDay.containsKey(day)) {
                schedulesByDay.put(day, new ArrayList<>());
            }
            schedulesByDay.get(day).add(schedule);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("schedules", schedulesByDay);
        return ResponseEntity.ok(response);
    }
}
