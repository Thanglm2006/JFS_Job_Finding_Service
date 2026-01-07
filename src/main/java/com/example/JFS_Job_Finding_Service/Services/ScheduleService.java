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
            return ResponseEntity.status(403).body("Access denied.");
        }

        Employer employer = jwtUtil.getEmployer(token);
        List<JobPost> jobs = jobPostRepository.findByEmployer(employer);
        List<PositionFrameDTO> response = new ArrayList<>();

        try {
            for (JobPost job : jobs) {
                List<PositionParserDTO> positions = objectMapper.readValue(
                        job.getPositions().toString(),
                        new TypeReference<List<PositionParserDTO>>() {}
                );

                for (PositionParserDTO pos : positions) {
                    List<JobShift> existingShifts = jobShiftRepository.findByJobId(job.getId())
                            .stream()
                            .filter(shift -> shift.getPositionName().equals(pos.getName()))
                            .collect(Collectors.toList());

                    List<JobShiftDTO> shiftDTOs = existingShifts.stream()
                            .map(s -> JobShiftDTO.builder()
                                    .id(s.getId())
                                    .day(s.getDay())
                                    .startTime(s.getStartTime())
                                    .endTime(s.getEndTime())
                                    .maxQuantity(s.getMaxQuantity())
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
            return ResponseEntity.status(500).body("Error parsing position data: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> updateFrame(String token, SaveFrameRequest request) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        JobPost job = jobPostRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getEmployer().getId().equals(jwtUtil.getEmployer(token).getId())) {
            return ResponseEntity.status(403).body("Unauthorized to modify this job.");
        }

        try {
            List<PositionParserDTO> positions = objectMapper.readValue(
                    job.getPositions().toString(),
                    new TypeReference<List<PositionParserDTO>>() {}
            );
            boolean positionExists = positions.stream().anyMatch(p -> p.getName().equals(request.getPositionName()));
            if (!positionExists) {
                return ResponseEntity.badRequest().body("Position does not exist in this job.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error validating position.");
        }

        for (JobShiftDTO dto : request.getShifts()) {
            if (dto.getStartTime() >= dto.getEndTime()) {
                return ResponseEntity.badRequest().body("Invalid time range: Start time must be before end time.");
            }
        }

        List<JobShift> existingShifts = jobShiftRepository.findByJobId(job.getId())
                .stream()
                .filter(s -> s.getPositionName().equals(request.getPositionName()))
                .collect(Collectors.toList());
        jobShiftRepository.deleteAll(existingShifts);

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
        return ResponseEntity.ok("Schedule frame updated successfully.");
    }

    public ResponseEntity<?> getFramesForApplicant(String token, String applicationId) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Application application = applicationsRepository.findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.status(404).body("Application not found.");
        }

        Applicant applicant = jwtUtil.getApplicant(token);
        return ResponseEntity.status(403).body("Unauthorized access to this application.");

    }

    @Transactional
    public ResponseEntity<?> registerShifts(String token, RegisterShiftsRequest request) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkWhetherIsApplicant(token)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Applicant applicant = jwtUtil.getApplicant(token);

        if (request.getShiftIds().size() <= 3) {
            return ResponseEntity.badRequest().body("You must register for more than 3 shifts.");
        }

        List<JobShift> selectedShifts = jobShiftRepository.findAllById(request.getShiftIds());
        if (selectedShifts.size() != request.getShiftIds().size()) {
            return ResponseEntity.badRequest().body("Some selected shifts do not exist.");
        }

        for (int i = 0; i < selectedShifts.size(); i++) {
            for (int j = i + 1; j < selectedShifts.size(); j++) {
                JobShift s1 = selectedShifts.get(i);
                JobShift s2 = selectedShifts.get(j);
                if (isOverlapping(s1.getDay(), s1.getStartTime(), s1.getEndTime(), s2.getDay(), s2.getStartTime(), s2.getEndTime())) {
                    return ResponseEntity.badRequest().body("Conflict within selected shifts: " + s1.getDay() + " and " + s2.getDay());
                }
            }
        }

        List<Schedule> currentSchedules = scheduleRepository.findByApplicant(applicant);
        for (JobShift selected : selectedShifts) {
            for (Schedule existing : currentSchedules) {
                if (isOverlapping(selected.getDay(), selected.getStartTime(), selected.getEndTime(), existing.getDay(), existing.getStartTime(), existing.getEndTime())) {
                    return ResponseEntity.badRequest().body("Conflict with existing schedule: " + existing.getDay() + " at " + existing.getStartTime() + " - " + existing.getEndTime());
                }
            }
        }

        List<ShiftApplication> applications = selectedShifts.stream()
                .map(shift -> {
                    if (shiftApplicationRepository.existsByJobShiftAndApplicant(shift, applicant)) {
                        throw new RuntimeException("Already applied for shift ID: " + shift.getId());
                    }
                    return ShiftApplication.builder()
                            .jobShift(shift)
                            .applicant(applicant)
                            .status(ShiftApplication.Status.PENDING)
                            .build();
                })
                .collect(Collectors.toList());

        try{
            shiftApplicationRepository.saveAll(applications);
        } catch (Exception e){
            return ResponseEntity.badRequest().body("Error saving applications, possibly duplicate request.");
        }

        return ResponseEntity.ok("Shifts registered successfully.");
    }

    private boolean isOverlapping(String day1, int start1, int end1, String day2, int start2, int end2) {
        if (!day1.equals(day2)) return false;
        return Math.max(start1, start2) < Math.min(end1, end2);
    }
}