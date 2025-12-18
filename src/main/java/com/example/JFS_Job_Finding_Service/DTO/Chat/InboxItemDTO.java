package com.example.JFS_Job_Finding_Service.DTO.Chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InboxItemDTO {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private String status;

    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private boolean isRead;
}