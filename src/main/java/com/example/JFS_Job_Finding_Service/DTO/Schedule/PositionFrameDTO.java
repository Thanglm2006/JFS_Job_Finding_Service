package com.example.JFS_Job_Finding_Service.DTO.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PositionFrameDTO {
    private String jobId;
    private String jobTitle;
    private String positionName;
    private List<JobShiftDTO> shifts;

}