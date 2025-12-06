package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Auth.ApplicantResponse;
import com.example.JFS_Job_Finding_Service.Services.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller("/api/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyForJob(
            @RequestHeader HttpHeaders headers,
            @RequestParam("jobId") Long jobId,
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
}
