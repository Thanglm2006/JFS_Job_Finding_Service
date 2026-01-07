package com.example.JFS_Job_Finding_Service.DTO.Schedule;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ShiftApplicationResponse {
    private Long id;
    private String applicantName;
    private String applicantId;
    private String positionName;
    private String day;
    private int startTime;
    private int endTime;
    private String status;
    private Instant appliedAt;
}