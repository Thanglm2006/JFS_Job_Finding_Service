package com.example.JFS_Job_Finding_Service.DTO;

import java.util.List;

public class SetScheduleRequest {
    private String applicantId;
    private String jobId;
    private List<Schedule> schedules;

    // Getters and setters
    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }
}