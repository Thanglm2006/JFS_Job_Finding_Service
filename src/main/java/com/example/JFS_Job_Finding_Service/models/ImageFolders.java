package com.example.JFS_Job_Finding_Service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "image_folders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageFolders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name="folder_name")
    private String folderName;

    @Column(nullable = false, name="file_name")
    private String fileName;
}
