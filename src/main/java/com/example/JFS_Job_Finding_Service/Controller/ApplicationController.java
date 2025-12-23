package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Application.ApplyDTO;
import com.example.JFS_Job_Finding_Service.DTO.Auth.ApplicantResponse;
import com.example.JFS_Job_Finding_Service.DTO.SetScheduleRequest;
import com.example.JFS_Job_Finding_Service.Services.ApplicationService;
import com.example.JFS_Job_Finding_Service.Services.EmployeeService;
import com.example.JFS_Job_Finding_Service.Services.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/apply")
    @Operation(summary = "apply for a post, only applicant can do this")
    public ResponseEntity<?> applyForJob(
            @RequestHeader HttpHeaders headers,
            @ModelAttribute ApplyDTO dto
    ) {
        return applicationService.applyForJob(headers.getFirst("token"), dto);
    }
    @PostMapping("/accept")
    public ResponseEntity<?> acceptApplication(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return applicationService.accept(headers.getFirst("token"), applicantResponse.getJobId(), applicantResponse.getApplicantId(), applicantResponse.getInterview());
    }
    @PostMapping("/reject")
    public ResponseEntity<?> rejectApplication(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return applicationService.reject(headers.getFirst("token"), applicantResponse.getJobId(), applicantResponse.getApplicantId(), applicantResponse.getReason());
    }
    @GetMapping("/unApply")
    @Operation(summary = "unapply for a post, only applicant can do this")
    public ResponseEntity<?> unApply(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") String jobId
    ) {
        try {
            return applicationService.unApplyForJob(headers.getFirst("token"), jobId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error unapplying for post: " + e.getMessage());
        }
    }
    @PostMapping("/setSchedule")
    public ResponseEntity<?> setSchedule(
            @RequestHeader HttpHeaders headers,
            @RequestBody SetScheduleRequest setScheduleRequest
            ) {
        return scheduleService.setSchedule(headers.getFirst("token"), setScheduleRequest.getApplicantId(), setScheduleRequest.getJobId(), setScheduleRequest.getSchedules());
    }
    @GetMapping("getStaffsForEmployer")
    public ResponseEntity<?> getStaffsForEmployer(
            @RequestHeader HttpHeaders headers
    ) {
        return employeeService.getStaffsForEmployer(headers.getFirst("token"));
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
        return scheduleService.getSchedulesForApplicant(headers.getFirst("token"));
    }
    @PostMapping("deleteStaff")
    public ResponseEntity<?> deleteStaff(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return employeeService.deletestaff(headers.getFirst("token"),  applicantResponse.getApplicantId(),applicantResponse.getJobId());
    }
}
