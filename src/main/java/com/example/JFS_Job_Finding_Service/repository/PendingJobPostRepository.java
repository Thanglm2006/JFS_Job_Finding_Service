package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.PendingJobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingJobPostRepository extends JpaRepository<PendingJobPost, Long> {
}
