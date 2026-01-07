package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Application;
import com.example.JFS_Job_Finding_Service.models.Enum.ApplicationStatus;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.models.User;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    List<Application> findByApplicant(Applicant applicant);

    Page<Application> findByApplicant(Applicant applicant, Pageable pageable);

    List<Application> findByJob(JobPost jobPost);

    List<Application> findByJobAndStatus(JobPost jobPost, ApplicationStatus status);

    Optional<Application> findByJobAndApplicant(JobPost jobPost, Applicant applicant);
}
