package com.example.JFS_Job_Finding_Service.DTO.Schedule;

import lombok.Data;
import java.util.List;

@Data
public class SaveFrameRequest {
    private String jobId;
    private String positionName;
    private List<JobShiftDTO> shifts;
}