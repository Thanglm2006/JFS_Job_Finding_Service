package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Schedule.RegisterShiftsRequest;
import com.example.JFS_Job_Finding_Service.DTO.Schedule.SaveFrameRequest;
import com.example.JFS_Job_Finding_Service.Services.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/employer/frames")
    public ResponseEntity<?> getPositionAndScheduleFrame(
            @RequestHeader HttpHeaders headers
    ) {
        return scheduleService.getPositionAndScheduleFrame(headers.getFirst("token"));
    }

    @PostMapping("/employer/updateFrame")
    public ResponseEntity<?> updateFrame(
            @RequestHeader HttpHeaders headers,
            @RequestBody SaveFrameRequest request
    ) {
        return scheduleService.updateFrame(headers.getFirst("token"), request);
    }

    @GetMapping("/applicant/frames")
    public ResponseEntity<?> getFramesForApplicant(
            @RequestHeader HttpHeaders headers,
            @RequestParam String applicationId
    ) {
        return scheduleService.getFramesForApplicant(headers.getFirst("token"), applicationId);
    }

    @PostMapping("/applicant/registerShifts")
    public ResponseEntity<?> registerShifts(
            @RequestHeader HttpHeaders headers,
            @RequestBody RegisterShiftsRequest request
    ) {
        return scheduleService.registerShifts(headers.getFirst("token"), request);
    }
}