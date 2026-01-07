package com.example.JFS_Job_Finding_Service.DTO.Schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobShiftDTO {
    private Long id;
    private String day;
    private int startTime;
    private int endTime;
    private int maxQuantity;
    private String description;
}