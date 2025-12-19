package com.example.JFS_Job_Finding_Service.repository;

import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Employer;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, String> {
    Page<JobPost> findByEmployer(Employer employer, Pageable pageable);
    List<JobPost> findByEmployer(Employer employer);

    @Query(value = """
        SELECT * FROM filter_job_posts(
            :keyword, 
            CAST(:type AS job_type), 
            :address, 
            :minSalary, 
            :maxSalary, 
            :limit, 
            :offset
        )
        """, nativeQuery = true)
    List<JobPost> searchWithPGroonga(
            @Param("keyword") String keyword,
            @Param("type") String type,       // String will be cast to enum in SQL
            @Param("address") String address, // Single string address
            @Param("minSalary") BigDecimal minSalary,
            @Param("maxSalary") BigDecimal maxSalary,
            @Param("limit") int limit,
            @Param("offset") int offset
    );


}
