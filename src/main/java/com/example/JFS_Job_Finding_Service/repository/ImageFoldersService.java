package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.ImageFolders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageFoldersService extends JpaRepository<ImageFolders, Long> {
    ImageFolders findByFolderName(String folderName);
}
