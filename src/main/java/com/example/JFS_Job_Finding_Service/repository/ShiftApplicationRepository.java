package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.JobShift;
import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.ShiftApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ShiftApplicationRepository extends JpaRepository<ShiftApplication, Long> {
    boolean existsByJobShiftAndApplicant(JobShift jobShift, Applicant applicant);
    long countByJobShiftAndStatus(JobShift jobShift, ShiftApplication.Status status);
    List<ShiftApplication> findByApplicantId(String applicantId);

    Collection<? extends ShiftApplication> findByJobShift(JobShift shift);
}