package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {
}
