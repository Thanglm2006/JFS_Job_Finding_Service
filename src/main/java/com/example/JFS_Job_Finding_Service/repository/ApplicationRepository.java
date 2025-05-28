package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Application;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    Optional<Application> findByApplicant(Applicant applicant);
    Application findByJob(JobPost jobPost);
    Optional<Application> findByJobAndApplicant(JobPost jobPost, Applicant applicant);
}
