package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.JobShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobShiftRepository extends JpaRepository<JobShift, Long> {
    List<JobShift> findByJobId(String jobId);
}