package com.example.JFS_Job_Finding_Service.DTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
public class message {
    private String message;
    private Instant time;
    private String type;

}
