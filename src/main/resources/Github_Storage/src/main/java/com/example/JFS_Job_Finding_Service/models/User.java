package com.example.JFS_Job_Finding_Service.models;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String phone;

    private String address;

    @Column(nullable = false)
    private String role;

    private String avatarUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    User(String name, String email, String password, String phone, String address, String role, String avatarUrl) {
        this.fullName = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }
    User(String name, String email, String password, String phone, String role, String avatarUrl) {
        this.fullName = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }
    User(String name, String email, String password, String role, String avatarUrl) {
        this.fullName = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }
}
