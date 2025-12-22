package com.example.JFS_Job_Finding_Service.models.POJO;

import com.example.JFS_Job_Finding_Service.models.Enum.PositionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPosition implements Serializable {
    private String name;
    private Integer quantity;
    private PositionStatus status=PositionStatus.OPEN;
}