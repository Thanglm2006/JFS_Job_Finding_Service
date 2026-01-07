package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Schedule.*;
import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.models.Schedule;
import com.example.JFS_Job_Finding_Service.repository.*;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired private JobShiftRepository jobShiftRepository;
    @Autowired private ShiftApplicationRepository shiftApplicationRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private JobPostRepository jobPostRepository;
    @Autowired private ApplicationRepository applicationsRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TokenService tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<?> getPositionAndScheduleFrame(String token) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        Employer employer = jwtUtil.getEmployer(token);
        List<JobPost> jobs = jobPostRepository.findByEmployer(employer);
        List<PositionFrameDTO> response = new ArrayList<>();

        try {
            for (JobPost job : jobs) {
                String jsonPositions = objectMapper.writeValueAsString(job.getPositions());
                List<PositionParserDTO> positions = objectMapper.readValue(
                        jsonPositions,
                        new TypeReference<List<PositionParserDTO>>() {}
                );

                Set<String> processedPositions = new HashSet<>();
                List<JobShift> allShifts = jobShiftRepository.findByJobId(job.getId());

                for (PositionParserDTO pos : positions) {
                    if (processedPositions.contains(pos.getName().toLowerCase())) {
                        continue;
                    }
                    processedPositions.add(pos.getName().toLowerCase());

                    List<JobShiftDTO> shiftDTOs = allShifts.stream()
                            .filter(shift -> shift.getPositionName().equalsIgnoreCase(pos.getName()))
                            .map(s -> JobShiftDTO.builder()
                                    .id(s.getId())
                                    .day(s.getDay())
                                    .startTime(s.getStartTime())
                                    .endTime(s.getEndTime())
                                    .maxQuantity(s.getMaxQuantity())
                                    .currentQuantity((int) shiftApplicationRepository.countByJobShiftAndStatus(s, ShiftApplication.Status.APPROVED))
                                    .description(s.getDescription())
                                    .build())
                            .collect(Collectors.toList());

                    response.add(PositionFrameDTO.builder()
                            .jobId(job.getId())
                            .jobTitle(job.getTitle())
                            .positionName(pos.getName())
                            .shifts(shiftDTOs)
                            .build());
                }
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi khi xử lý dữ liệu vị trí: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> updateFrame(String token, SaveFrameRequest request) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        JobPost job = jobPostRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc."));

        if (!job.getEmployer().getId().equals(jwtUtil.getEmployer(token).getId())) {
            return ResponseEntity.status(403).body("Bạn không có quyền chỉnh sửa công việc này.");
        }

        try {
            String jsonPositions = objectMapper.writeValueAsString(job.getPositions());
            List<PositionParserDTO> positions = objectMapper.readValue(
                    jsonPositions,
                    new TypeReference<List<PositionParserDTO>>() {}
            );
            boolean positionExists = positions.stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(request.getPositionName()));
            if (!positionExists) {
                return ResponseEntity.badRequest().body("Vị trí không tồn tại trong bài đăng tuyển dụng này.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi xác thực vị trí.");
        }

        for (JobShiftDTO dto : request.getShifts()) {
            if (dto.getStartTime() >= dto.getEndTime()) {
                return ResponseEntity.badRequest().body("Thời gian không hợp lệ: Giờ bắt đầu phải nhỏ hơn giờ kết thúc.");
            }
            if (dto.getMaxQuantity() <= 0) {
                return ResponseEntity.badRequest().body("Số lượng nhân sự tối đa phải lớn hơn 0.");
            }
        }

        List<JobShift> existingShifts = jobShiftRepository.findByJobId(job.getId())
                .stream()
                .filter(s -> s.getPositionName().equalsIgnoreCase(request.getPositionName()))
                .collect(Collectors.toList());

        for (JobShift shift : existingShifts) {
            List<ShiftApplication> relatedApplications = shiftApplicationRepository.findByJobShift(shift);

            for (ShiftApplication app : relatedApplications) {
                sendNotification(app.getApplicant().getUser(),
                        "Ca làm việc " + shift.getDay() + " (" + shift.getStartTime() + "-" + shift.getEndTime() +
                                ") tại công việc " + job.getTitle() + " đã bị hủy do thay đổi lịch trình từ nhà tuyển dụng.");
            }

            shiftApplicationRepository.deleteAll(relatedApplications);
        }

        if (!existingShifts.isEmpty()) {
            jobShiftRepository.deleteAll(existingShifts);
        }

        List<JobShift> newShifts = request.getShifts().stream()
                .map(dto -> JobShift.builder()
                        .job(job)
                        .positionName(request.getPositionName())
                        .day(dto.getDay())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .maxQuantity(dto.getMaxQuantity())
                        .description(dto.getDescription())
                        .build())
                .collect(Collectors.toList());

        jobShiftRepository.saveAll(newShifts);
        return ResponseEntity.ok("Cập nhật khung lịch làm việc thành công. Các ca cũ và đơn đăng ký liên quan đã được xóa.");
    }

    public ResponseEntity<?> getStaffsInShift(String token, String jobId, String positionName) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        JobPost job = jobPostRepository.findById(jobId).orElse(null);
        if (job == null || !job.getEmployer().getId().equals(jwtUtil.getEmployer(token).getId())) {
            return ResponseEntity.status(403).body("Không tìm thấy công việc hoặc không có quyền.");
        }

        List<JobShift> shifts = jobShiftRepository.findByJobId(jobId).stream()
                .filter(s -> s.getPositionName().equalsIgnoreCase(positionName))
                .collect(Collectors.toList());

        List<Map<String, Object>> response = new ArrayList<>();

        for (JobShift shift : shifts) {
            List<ShiftApplication> approvedApps = shiftApplicationRepository.findByJobShiftAndStatus(shift, ShiftApplication.Status.APPROVED);

            List<Map<String, Object>> staffs = approvedApps.stream()
                    .map(app -> {
                        Map<String, Object> staffInfo = new HashMap<>();
                        staffInfo.put("applicantId", app.getApplicant().getId());
                        staffInfo.put("fullName", app.getApplicant().getUser().getFullName());
                        staffInfo.put("email", app.getApplicant().getUser().getEmail());
                        staffInfo.put("phone", app.getApplicant().getUser().getPhone());
                        staffInfo.put("avatar", app.getApplicant().getUser().getAvatarUrl());
                        return staffInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> shiftData = new HashMap<>();
            shiftData.put("shiftId", shift.getId());
            shiftData.put("day", shift.getDay());
            shiftData.put("startTime", shift.getStartTime());
            shiftData.put("endTime", shift.getEndTime());
            shiftData.put("maxQuantity", shift.getMaxQuantity());
            shiftData.put("currentQuantity", staffs.size());
            shiftData.put("staffs", staffs);

            response.add(shiftData);
        }

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getFramesForApplicant(String token, String applicationId) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        Application application = applicationsRepository.findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.status(404).body("Không tìm thấy đơn ứng tuyển.");
        }

        Applicant applicant = jwtUtil.getApplicant(token);
        if (!application.getApplicant().getId().equals(applicant.getId())) {
            return ResponseEntity.status(403).body("Bạn không có quyền truy cập đơn ứng tuyển này.");
        }

        List<JobShift> shifts = jobShiftRepository.findByJobId(application.getJob().getId())
                .stream()
                .filter(s -> s.getPositionName().equalsIgnoreCase(application.getPosition()))
                .collect(Collectors.toList());

        List<JobShiftDTO> response = shifts.stream()
                .map(s -> JobShiftDTO.builder()
                        .id(s.getId())
                        .day(s.getDay())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .maxQuantity(s.getMaxQuantity())
                        .currentQuantity((int) shiftApplicationRepository.countByJobShiftAndStatus(s, ShiftApplication.Status.APPROVED))
                        .description(s.getDescription())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getApplicantApprovedSchedules(String token) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        Applicant applicant = jwtUtil.getApplicant(token);
        List<Schedule> schedules = scheduleRepository.findByApplicant(applicant);

        var response = schedules.stream()
                .map(s -> new HashMap<String, Object>() {{
                    put("id", s.getId());
                    put("jobTitle", s.getJob().getTitle());
                    put("employerName", s.getJob().getEmployer().getOrgName());
                    put("day", s.getDay());
                    put("startTime", s.getStartTime());
                    put("endTime", s.getEndTime());
                    put("description", s.getDescription());
                    put("status", "APPROVED");
                }})
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> registerShifts(String token, RegisterShiftsRequest request) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        Applicant applicant = jwtUtil.getApplicant(token);

        Application application = applicationsRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển."));

        if (!application.getApplicant().getId().equals(applicant.getId())) {
            return ResponseEntity.status(403).body("Đơn ứng tuyển không hợp lệ.");
        }

        if (request.getShiftIds() == null || request.getShiftIds().size() <= 3) {
            return ResponseEntity.badRequest().body("Bạn phải đăng ký nhiều hơn 3 ca làm việc.");
        }

        List<JobShift> selectedShifts = jobShiftRepository.findAllById(request.getShiftIds());
        if (selectedShifts.size() != request.getShiftIds().size()) {
            return ResponseEntity.badRequest().body("Một số ca làm việc đã chọn không tồn tại.");
        }

        for (JobShift s : selectedShifts) {
            if (!s.getPositionName().equalsIgnoreCase(application.getPosition()) || !s.getJob().getId().equals(application.getJob().getId())) {
                return ResponseEntity.badRequest().body("Ca làm việc không thuộc vị trí hoặc công việc bạn đã ứng tuyển.");
            }
        }

        for (int i = 0; i < selectedShifts.size(); i++) {
            for (int j = i + 1; j < selectedShifts.size(); j++) {
                JobShift s1 = selectedShifts.get(i);
                JobShift s2 = selectedShifts.get(j);
                if (isOverlapping(s1.getDay(), s1.getStartTime(), s1.getEndTime(), s2.getDay(), s2.getStartTime(), s2.getEndTime())) {
                    return ResponseEntity.badRequest().body("Xung đột thời gian giữa các ca đã chọn: " + s1.getDay() + " (" + s1.getStartTime() + "-" + s1.getEndTime() + ")");
                }
            }
        }

        List<Schedule> currentSchedules = scheduleRepository.findByApplicant(applicant);
        for (JobShift selected : selectedShifts) {
            for (Schedule existing : currentSchedules) {
                if (isOverlapping(selected.getDay(), selected.getStartTime(), selected.getEndTime(), existing.getDay(), existing.getStartTime(), existing.getEndTime())) {
                    return ResponseEntity.badRequest().body("Trùng lịch với công việc hiện tại: " + existing.getDay() + " (" + existing.getStartTime() + "-" + existing.getEndTime() + ")");
                }
            }
        }

        List<ShiftApplication> applications = selectedShifts.stream()
                .map(shift -> {
                    if (shiftApplicationRepository.existsByJobShiftAndApplicant(shift, applicant)) {
                        throw new RuntimeException("Bạn đã đăng ký ca này rồi: ID " + shift.getId());
                    }
                    return ShiftApplication.builder()
                            .jobShift(shift)
                            .applicant(applicant)
                            .status(ShiftApplication.Status.PENDING)
                            .appliedAt(Instant.now())
                            .build();
                })
                .collect(Collectors.toList());

        try {
            shiftApplicationRepository.saveAll(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lưu đăng ký, có thể bạn đã gửi trùng lặp.");
        }

        return ResponseEntity.ok("Đăng ký ca làm việc thành công, vui lòng chờ duyệt.");
    }

    public ResponseEntity<?> getShiftApplicationsForEmployer(String token, String jobId, String positionName) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        JobPost job = jobPostRepository.findById(jobId).orElse(null);
        if (job == null || !job.getEmployer().getId().equals(jwtUtil.getEmployer(token).getId())) {
            return ResponseEntity.status(403).body("Không tìm thấy công việc hoặc không có quyền.");
        }

        List<JobShift> jobShifts = jobShiftRepository.findByJobId(jobId).stream()
                .filter(shift -> shift.getPositionName().equalsIgnoreCase(positionName))
                .collect(Collectors.toList());

        List<ShiftApplication> applications = new ArrayList<>();
        for (JobShift shift : jobShifts) {
            applications.addAll(shiftApplicationRepository.findByJobShift(shift));
        }

        List<ShiftApplicationResponse> response = applications.stream()
                .map(app -> ShiftApplicationResponse.builder()
                        .id(app.getId())
                        .applicantName(app.getApplicant().getUser().getFullName())
                        .applicantId(app.getApplicant().getId())
                        .positionName(app.getJobShift().getPositionName())
                        .day(app.getJobShift().getDay())
                        .startTime(app.getJobShift().getStartTime())
                        .endTime(app.getJobShift().getEndTime())
                        .status(app.getStatus().toString())
                        .appliedAt(app.getAppliedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> reviewShiftApplication(String token, ReviewShiftRequest request) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Truy cập bị từ chối.");
        }

        ShiftApplication app = shiftApplicationRepository.findById(request.getShiftApplicationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đăng ký ca."));

        if (!app.getJobShift().getJob().getEmployer().getId().equals(jwtUtil.getEmployer(token).getId())) {
            return ResponseEntity.status(403).body("Bạn không có quyền xử lý yêu cầu này.");
        }

        if (request.isApproved()) {
            long currentApproved = shiftApplicationRepository.countByJobShiftAndStatus(app.getJobShift(), ShiftApplication.Status.APPROVED);
            if (currentApproved >= app.getJobShift().getMaxQuantity()) {
                return ResponseEntity.badRequest().body("Ca làm việc này đã đủ số lượng nhân sự.");
            }
            app.setStatus(ShiftApplication.Status.APPROVED);

            Schedule schedule = Schedule.builder()
                    .applicant(app.getApplicant())
                    .job(app.getJobShift().getJob())
                    .day(app.getJobShift().getDay())
                    .startTime(app.getJobShift().getStartTime())
                    .endTime(app.getJobShift().getEndTime())
                    .description("Vị trí: " + app.getJobShift().getPositionName() + ". " +
                            (app.getJobShift().getDescription() != null ? app.getJobShift().getDescription() : ""))
                    .build();
            scheduleRepository.save(schedule);

            sendNotification(app.getApplicant().getUser(), "Đăng ký ca làm việc của bạn (" + app.getJobShift().getDay() + ") đã được chấp nhận.");
        } else {
            app.setStatus(ShiftApplication.Status.REJECTED);
            sendNotification(app.getApplicant().getUser(), "Đăng ký ca làm việc của bạn (" + app.getJobShift().getDay() + ") đã bị từ chối.");
        }

        app.setUpdatedAt(Instant.now());
        shiftApplicationRepository.save(app);

        return ResponseEntity.ok(request.isApproved() ? "Đã duyệt ca làm việc." : "Đã từ chối ca làm việc.");
    }

    private boolean isOverlapping(String day1, int start1, int end1, String day2, int start2, int end2) {
        if (!day1.equals(day2)) return false;
        return Math.max(start1, start2) < Math.min(end1, end2);
    }

    private void sendNotification(User user, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }
}