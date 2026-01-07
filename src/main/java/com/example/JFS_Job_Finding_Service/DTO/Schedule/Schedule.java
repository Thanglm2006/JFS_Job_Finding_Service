package com.example.JFS_Job_Finding_Service.DTO.Schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
        private String day;
        private int startTime;
        private int endTime;
        private String description;
}
