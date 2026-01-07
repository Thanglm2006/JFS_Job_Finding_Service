package com.example.JFS_Job_Finding_Service.DTO.Schedule;

import lombok.Data;
import java.util.List;

@Data
public class RegisterShiftsRequest {
    private String applicationId;
    private List<Long> shiftIds;
}