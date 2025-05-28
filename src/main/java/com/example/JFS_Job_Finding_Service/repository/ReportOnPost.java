package com.example.JFS_Job_Finding_Service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportOnPost extends JpaRepository<ReportOnPost, Long> {
}
