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
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền thiết lập lịch trình.");
        }
        if (schedules == null || schedules.isEmpty()) {
            return ResponseEntity.badRequest().body("Lịch làm việc không được để trống.");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getStartTime() >= schedule.getEndTime())) {
            return ResponseEntity.badRequest().body("Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc.");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getDay() == null || !schedule.getDay().matches("^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)$"))) {
            return ResponseEntity.badRequest().body("Thứ trong tuần không hợp lệ.");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getDescription() == null || schedule.getDescription().isEmpty())) {
            return ResponseEntity.badRequest().body("Mô tả lịch trình không được để trống.");
        }
        if (schedules.stream().anyMatch(schedule -> schedule.getStartTime() < 0 || schedule.getEndTime() < 0)) {
            return ResponseEntity.badRequest().body("Thời gian phải là số dương.");
        }
        for (int i = 0; i < schedules.size(); i++) {
            Schedule s1 = schedules.get(i);
            for (int j = i + 1; j < schedules.size(); j++) {
                Schedule s2 = schedules.get(j);
                if (s1.getDay().equals(s2.getDay()) &&
                        s1.getStartTime() <= s2.getEndTime() &&
                        s2.getStartTime() < s1.getEndTime()) {
                    return ResponseEntity.badRequest().body("Lịch làm việc bị trùng lặp vào " + s1.getDay() + " trong khoảng " + s1.getStartTime() + "h - " + s1.getEndTime() + "h.");
                }
            }
        }
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên với ID: " + applicantId));
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + jobId));
        List<com.example.JFS_Job_Finding_Service.models.Schedule> schedulesForJob = scheduleRepository.findByJob(job);
        if (schedulesForJob != null && !schedulesForJob.isEmpty()) {
            scheduleRepository.deleteAll(schedulesForJob);
        }
        List<com.example.JFS_Job_Finding_Service.models.Schedule> existingSchedules = scheduleRepository.findByApplicant(applicant);

        for( Schedule schedule : schedules) {
            if (schedule.getStartTime()>=(schedule.getEndTime())) {
                return ResponseEntity.badRequest().body("Thời gian bắt đầu không thể sau thời gian kết thúc.");
            }
            if(schedule.getDay()!=null && !schedule.getDay().matches("^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)$")) {
                return ResponseEntity.badRequest().body("Ngày không hợp lệ: " + schedule.getDay());
            }
            for( com.example.JFS_Job_Finding_Service.models.Schedule existingSchedule : existingSchedules) {
                if (existingSchedule.getDay().equals(schedule.getDay()) &&
                        ((schedule.getStartTime() >= existingSchedule.getStartTime() && schedule.getStartTime() <= existingSchedule.getEndTime()) ||
                                (schedule.getEndTime() >= existingSchedule.getStartTime() && schedule.getEndTime() <= existingSchedule.getEndTime()))) {
                    return ResponseEntity.badRequest().body("Lịch trình bị trùng lặp với lịch hiện có vào " + schedule.getDay());
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
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Không thể lưu lịch trình, dữ liệu không hợp lệ.");
            }
        }
        Notification notification = new Notification();
        notification.setUser(applicant.getUser());
        notification.setMessage("Lịch làm việc cho công việc " + job.getTitle() + " đã được cập nhật bởi " + job.getEmployer().getOrgName());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return ResponseEntity.status(200).body("Đã lưu lịch trình làm việc thành công.");
    }
    public ResponseEntity<?> getSchedulesForApplicant(String token){
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền xem lịch trình.");
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            return ResponseEntity.status(404).body("Không tìm thấy thông tin ứng viên.");
        }
        List<com.example.JFS_Job_Finding_Service.models.Schedule> schedules = scheduleRepository.findByApplicant(applicant);
        schedules.sort(Comparator.comparingInt(com.example.JFS_Job_Finding_Service.models.Schedule::getStartTime));
        if (schedules.isEmpty()) {
            return ResponseEntity.ok("Bạn hiện chưa có lịch trình làm việc nào.");
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
    public ResponseEntity<?> getSchedulesForEmployer(String token, String ApplicantId){
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền xem lịch trình.");
        }
        Applicant applicant = applicantRepository.findById(ApplicantId).orElse(null);
        if (applicant == null) {
            return ResponseEntity.status(404).body("Không tìm thấy thông tin ứng viên.");
        }
        List<com.example.JFS_Job_Finding_Service.models.Schedule> schedules = scheduleRepository.findByApplicant(applicant);
        schedules.sort(Comparator.comparingInt(com.example.JFS_Job_Finding_Service.models.Schedule::getStartTime));
        if (schedules.isEmpty()) {
            return ResponseEntity.ok("Applicant chưa có lịch trình làm việc nào.");
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