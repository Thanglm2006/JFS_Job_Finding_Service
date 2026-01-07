package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Application.ApplicantResponse;
import com.example.JFS_Job_Finding_Service.DTO.Application.ApplyDTO;
import com.example.JFS_Job_Finding_Service.DTO.Schedule.JobShiftDTO;
import com.example.JFS_Job_Finding_Service.Services.ApplicationService;
import com.example.JFS_Job_Finding_Service.Services.EmployeeService;
import com.example.JFS_Job_Finding_Service.Services.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/acceptToInterview")
    public ResponseEntity<?> acceptApplication(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) throws MessagingException {
        return applicationService.acceptToInterview(headers.getFirst("token"), applicantResponse.getJobId(), applicantResponse.getApplicantId(), applicantResponse.getInterview());
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse) {
        return applicationService.accept(headers.getFirst("token"), applicantResponse.getJobId(), applicantResponse.getApplicantId());
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

    @GetMapping("/getReviewedApplicants")
    public ResponseEntity<?> getReviewedApplicants(@RequestHeader HttpHeaders headers) {
        return applicationService.getReviewedApplications(headers.getFirst("token"));
    }

    @GetMapping("/getStaffsForEmployer")
    public ResponseEntity<?> getStaffsForEmployer(
            @RequestHeader HttpHeaders headers
    ) {
        return employeeService.getStaffsForEmployer(headers.getFirst("token"));
    }

    @GetMapping("/getJobs")
    public ResponseEntity<?> getJobs(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return applicationService.getJobs(headers.getFirst("token"), page, size);
    }

    @PostMapping("/deleteStaff")
    public ResponseEntity<?> deleteStaff(
            @RequestHeader HttpHeaders headers,
            @RequestBody ApplicantResponse applicantResponse
    ) {
        return employeeService.deletestaff(headers.getFirst("token"), applicantResponse.getApplicantId(), applicantResponse.getJobId());
    }

}