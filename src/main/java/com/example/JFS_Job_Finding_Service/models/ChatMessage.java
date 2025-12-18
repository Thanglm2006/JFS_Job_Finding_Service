package com.example.JFS_Job_Finding_Service.models;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column()
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    @Column(name="file_url", nullable=true)
    private String fileUrl;

    public ChatMessage(User sender, User receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }
    public ChatMessage(User sender, User receiver, String message, String fileUrl) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.fileUrl = fileUrl;
    }

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

}
