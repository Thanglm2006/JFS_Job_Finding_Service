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

import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, String> {
    Page<JobPost> findByEmployer(Employer employer, Pageable pageable);
    List<JobPost> findByEmployer(Employer employer);
    @Query(value = """
        SELECT * FROM job_post
        WHERE (title || ' ' || description) &@~ :pattern
        LIMIT 10
        """, nativeQuery = true)
    List<JobPost> searchWithPGroonga(@Param("pattern") String pattern);
}
