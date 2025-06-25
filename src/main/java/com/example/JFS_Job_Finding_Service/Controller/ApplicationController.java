package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.ApplicantResponse;
import com.example.JFS_Job_Finding_Service.DTO.Schedule;
import com.example.JFS_Job_Finding_Service.DTO.SetScheduleRequest;
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
        return applicationService.accept(headers.getFirst("token"), applicantResponse.getJobId(), applicantResponse.getApplicantId());
    }
    @PostMapping("/reject")
    public ResponseEntity<?> rejectApplication(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return applicationService.reject(headers.getFirst("token"), applicantResponse.getJobId(), applicantResponse.getApplicantId());
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
            @RequestBody SetScheduleRequest setScheduleRequest
            ) {
        return applicationService.setSchedule(headers.getFirst("token"), setScheduleRequest.getApplicantId(), setScheduleRequest.getJobId(), setScheduleRequest.getSchedules());
    }
    @GetMapping("getStaffsForEmployer")
    public ResponseEntity<?> getStaffsForEmployer(
            @RequestHeader HttpHeaders headers
    ) {
        return applicationService.getStaffsForEmployer(headers.getFirst("token"));
    }
    @GetMapping("getJobs")
    public ResponseEntity<?> getJobs(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return applicationService.getJobs(headers.getFirst("token"), page, size);
    }
    @GetMapping("getSchedulesForApplicant")
    public ResponseEntity<?> getSchedulesForApplicant(
            @RequestHeader HttpHeaders headers
    ) {
        return applicationService.getSchedulesForApplicant(headers.getFirst("token"));
    }
    @PostMapping("deleteStaff")
    public ResponseEntity<?> deleteStaff(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return applicationService.deletestaff(headers.getFirst("token"),  applicantResponse.getApplicantId(),applicantResponse.getJobId());
    }
}
