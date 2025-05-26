package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name="admin")
@NoArgsConstructor
@AllArgsConstructor
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="full_name", nullable = false)
    private String fullName;
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;
}
