package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.ApplicantResponse;
import com.example.JFS_Job_Finding_Service.DTO.Schedule;
import com.example.JFS_Job_Finding_Service.Services.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;
    @PostMapping("/apply")
    public ResponseEntity<?> applyForJob(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId,
            @RequestParam("position") String position
    ) {
        return applicationService.applyForJob(headers.getFirst("token"), jobId, position);
    }
    @PostMapping("/accept")
    public ResponseEntity<?> acceptApplication(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return applicationService.accept(headers.getFirst("token"), applicantResponse.getApplicationId(), applicantResponse.getApplicantId());
    }
    @PostMapping("/reject")
    public ResponseEntity<?> rejectApplication(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return applicationService.reject(headers.getFirst("token"), applicantResponse.getApplicationId(), applicantResponse.getApplicantId());
    }
    @PostMapping("/getAllApplicationsForEmployer")
    public ResponseEntity<?> getAllApplicationsForEmployer(
            @RequestHeader HttpHeaders headers
    ) {
        return applicationService.getAllApplicationForEmployer(headers.getFirst("token"));
    }
    @PostMapping("/setSchedule")
    public ResponseEntity<?> setSchedule(
            @RequestHeader HttpHeaders headers,
            @RequestParam("applicantId") String applicantId,
            @RequestParam("jobId") String jobId,
            @RequestParam("schedules") List<Schedule> schedules
    ) {
        return applicationService.setSchedule(headers.getFirst("token"), applicantId, jobId,schedules);
    }

}
