package com.example.JFS_Job_Finding_Service.DTO.Schedule;

import lombok.Data;

@Data
public class ReviewShiftRequest {
    private Long shiftApplicationId;
    private boolean approved;
}