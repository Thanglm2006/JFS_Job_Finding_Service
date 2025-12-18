package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.models.Enum.ApplicationStatus;
import com.example.JFS_Job_Finding_Service.repository.*;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private ApplicantRepository  applicantRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TokenService tokenService;

    public ResponseEntity<?> getStaffsForEmployer(String token){
        HashMap<String, Object> response = new HashMap<>();
        if(!jwtUtil.checkPermission(token, "Employer")){
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền xem danh sách nhân sự.");
            return ResponseEntity.status(403).body(response);
        }
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Truy cập trái phép. Vui lòng đăng nhập lại.");
            return ResponseEntity.status(401).body(response);
        }
        Employer employer = jwtUtil.getEmployer(token);
        if (employer == null) {
            response.put("status", "fail");
            response.put("message", "Không tìm thấy thông tin nhà tuyển dụng.");
            return ResponseEntity.status(404).body(response);
        }
        List<JobPost> jobPosts = jobPostRepository.findByEmployer(employer);
        if (jobPosts.isEmpty()) {
            response.put("status", "fail");
            response.put("message", "Nhà tuyển dụng này hiện chưa có bài đăng nào.");
            return ResponseEntity.ok(response);
        }
        List<Map<String,Object>> staffs = new ArrayList<>();
        for(JobPost jobPost : jobPosts){
            List<Application> applications = applicationRepository.findByJobAndStatus(jobPost, "Accepted");
            for(Application application : applications) {
                Map<String, Object> staffData = new HashMap<>();
                Applicant applicant = application.getApplicant();
                staffData.put("applicantId", applicant.getId());
                staffData.put("applicantName", applicant.getUser().getFullName());
                staffData.put("avatar", applicant.getUser().getAvatarUrl());
                staffData.put("jobId", jobPost.getId());
                staffData.put("position", application.getPosition());
                staffData.put("appliedAt", application.getAppliedAt());
                staffData.put("applicantUserID", applicant.getUser().getId());
                List<com.example.JFS_Job_Finding_Service.models.Schedule> schedules = scheduleRepository.findByApplicantAndJob(applicant, jobPost);
                staffData.put("schedules", schedules);
                staffs.add(staffData);
            }
        }
        response.put("status", "success");
        response.put("staffs", staffs);
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> deletestaff(String token, String applicantId, String jobId){
        if(!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(401).body("Truy cập trái phép.");
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Bạn không có quyền thực hiện thao tác xóa nhân sự.");
        }
        Employer employer = jwtUtil.getEmployer(token);
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên với ID: " + applicantId));
        JobPost jobPost= jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + jobId));
        Application application= applicationRepository.findByJobAndApplicant(jobPost, applicant)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển cho công việc mã: " + jobId));
        if (application == null || !application.getStatus().equals(ApplicationStatus.ACCEPTED)) {
            return ResponseEntity.status(404).body("Không tìm thấy đơn ứng tuyển hợp lệ cho vị trí này.");

        }
        applicationRepository.delete(application);
        Notification notification = new Notification();
        notification.setUser(applicant.getUser());
        notification.setMessage("Hợp đồng làm việc vị trí " + application.getPosition() + " đã kết thúc bởi " + employer.getUser().getFullName());
        notification.setRead(false);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Đã xóa nhân sự khỏi hệ thống thành công.");
    }
}