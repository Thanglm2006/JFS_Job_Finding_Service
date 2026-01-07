package com.example.JFS_Job_Finding_Service.DTO.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftApplicationDTO {
    private Long id;
    private Long shiftId;
    private String applicantId;
    private String status;
    private Instant appliedAt;
    private JobShiftDTO shiftDetails;
}