package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Post.PostingRequest;
import com.example.JFS_Job_Finding_Service.models.JobPost;
import com.example.JFS_Job_Finding_Service.models.Notification;
import com.example.JFS_Job_Finding_Service.models.PendingJobPost;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import com.example.JFS_Job_Finding_Service.repository.PendingJobPostRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PendingJobPostService {
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    PendingJobPostRepository pendingJobPostRepository;
    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TokenService tokenService;

    public ResponseEntity<?> addPost(String token, PostingRequest postingRequest) {
        Map<String, Object> response = new HashMap<>();
        if(!tokenService.validateToken(token,jwtUtil.extractEmail(token))){
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        if(!jwtUtil.checkWhetherIsEmployer(token)){
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        PendingJobPost jobPost = new PendingJobPost();
        jobPost.setTitle(postingRequest.getTitle());
        jobPost.setEmployer(jwtUtil.getEmployer(token));
        jobPost.setDescription(postingRequest.getDescription());
        jobPost.setWorkspacePicture(postingRequest.getWorkSpacePicture());
        if(jobPost.getWorkspacePicture() != null){

        }
        pendingJobPostRepository.save(jobPost);
        Notification notification = new Notification();
        notification.setUser(jobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng của bạn đã được gửi đi và đang chờ duyệt: " + jobPost.getTitle());
        notification.setRead(false);
        notificationRepository.save(notification);
        response.put("jobPostId", jobPost.getId());
        response.put("status", "success");
        response.put("message", "Tạo bài đăng thành công, đang chờ duyệt!");
        response.put("jobPost", jobPost);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    public ResponseEntity<?> acceptPost(String token, Long pendingId){
        if(!jwtUtil.checkPermission(token, "Admin"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền truy cập");
        PendingJobPost pendingJobPost= pendingJobPostRepository.findById(pendingId).orElse(null);
        if(pendingJobPost == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại");
        }
        JobPost jobPost = new JobPost();
        jobPost.setTitle(pendingJobPost.getTitle());
        jobPost.setEmployer(pendingJobPost.getEmployer());
        jobPost.setDescription(pendingJobPost.getDescription());
        jobPost.setWorkspacePicture(pendingJobPost.getWorkspacePicture());
        jobPost.setCreatedAt(pendingJobPost.getCreatedAt());
        jobPostRepository.save(jobPost);
        pendingJobPostRepository.delete(pendingJobPost);
        Notification notification = new Notification();
        notification.setUser(pendingJobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng của bạn đã được duyệt: " + jobPost.getTitle());
        notification.setRead(false);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Bài đăng đã được duyệt thành công");
    }
}
