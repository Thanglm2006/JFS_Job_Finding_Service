package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Schedule.RegisterShiftsRequest;
import com.example.JFS_Job_Finding_Service.DTO.Schedule.ReviewShiftRequest;
import com.example.JFS_Job_Finding_Service.DTO.Schedule.SaveFrameRequest;
import com.example.JFS_Job_Finding_Service.Services.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;


    @GetMapping("/employer/frames")
    public ResponseEntity<?> getPositionAndScheduleFrame(@RequestHeader("token") String token) {
        return scheduleService.getPositionAndScheduleFrame(token);
    }

    @PostMapping("/employer/updateFrame")
    public ResponseEntity<?> updateFrame(
            @RequestHeader("token") String token,
            @RequestBody SaveFrameRequest request
    ) {
        return scheduleService.updateFrame(token, request);
    }

    @GetMapping("/applicant/my-schedules")
    public ResponseEntity<?> getApplicantApprovedSchedules(
            @RequestHeader("token") String token
    ) {
        return scheduleService.getApplicantApprovedSchedules(token);
    }

    @GetMapping("/employer/shift-applications")
    public ResponseEntity<?> getShiftApplications(
            @RequestHeader("token") String token,
            @RequestParam String jobId,
            @RequestParam String positionName
    ) {
        return scheduleService.getShiftApplicationsForEmployer(token, jobId, positionName);
    }

    @PostMapping("/employer/review-shift")
    public ResponseEntity<?> reviewShiftApplication(
            @RequestHeader("token") String token,
            @RequestBody ReviewShiftRequest request
    ) {
        return scheduleService.reviewShiftApplication(token, request);
    }


    @GetMapping("/applicant/frames")
    public ResponseEntity<?> getFramesForApplicant(
            @RequestHeader("token") String token,
            @RequestParam String applicationId
    ) {
        return scheduleService.getFramesForApplicant(token, applicationId);
    }

    @PostMapping("/applicant/registerShifts")
    public ResponseEntity<?> registerShifts(
            @RequestHeader("token") String token,
            @RequestBody RegisterShiftsRequest request
    ) {
        return scheduleService.registerShifts(token, request);
    }
}