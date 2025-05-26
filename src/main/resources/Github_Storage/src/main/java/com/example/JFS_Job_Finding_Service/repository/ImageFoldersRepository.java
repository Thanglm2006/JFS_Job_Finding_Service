package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.ImageFolders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageFoldersRepository extends JpaRepository<ImageFolders, Long> {
    List<ImageFolders> findByFolderName(String folderName);
}
