package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.EmployerRequest;
import com.example.JFS_Job_Finding_Service.models.Enum.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployerRequestRepository extends JpaRepository<EmployerRequest, Long> {
    List<EmployerRequest> findByStatus(VerificationStatus status);
    List<EmployerRequest> findAllByOrderByCreatedAtDesc();
}