package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.beans.BeanProperty;
import java.time.Instant;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull()
    @Column(name="message", nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;
    @Column(name="created_at", nullable=false)
    private Instant createdAt = Instant.now();
}

