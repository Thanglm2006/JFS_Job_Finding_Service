package com.example.JFS_Job_Finding_Service.DTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
public class message {
    private String content;
    private boolean isSent;
    private LocalDateTime timestamp;
    private String type;
    private String fileUrl;

    public message(String content, boolean isSent, LocalDateTime timestamp, String type) {
        this.content = content;
        this.isSent = isSent;
        this.timestamp = timestamp;
        this.type = type;
    }
}
