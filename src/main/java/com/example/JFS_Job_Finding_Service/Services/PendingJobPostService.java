package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Auth.PostingRequest;
import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.repository.ImageFoldersRepository;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import com.example.JFS_Job_Finding_Service.repository.PendingJobPostRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;

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
    private ImageFoldersRepository imageFoldersRepository;
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
        //Check scam, spam post
        try {
            String rawDesc = postingRequest.getDescription().toString();

            String processedDesc = Arrays.stream(rawDesc.replace("{", "").replace("}", "").split(", "))
                    .map(part -> {
                        int idx = part.indexOf("=");
                        return (idx != -1) ? part.substring(idx + 1).trim() : part.trim();
                    })
                    .collect(Collectors.joining(". "));
            String textToCheck = postingRequest.getTitle() + " . " + processedDesc;
            String apiUrl = "http://host.docker.internal:8000/predict";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", textToCheck);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(apiUrl, requestBody, Map.class);

            if (apiResponse.getBody() != null) {
                Map<String, Object> body = apiResponse.getBody();

                if (body.containsKey("scores")) {
                    Map<String, Double> scores = (Map<String, Double>) body.get("scores");
                    Double scamScore = scores.get("scam");


                    if (scamScore > 0.7) {
                        System.out.println("❌ Blocked Scam Post: " + scamScore);

                        response.put("status", "fail");
                        response.put("message", "Bài đăng của bạn bị hệ thống chặn do nghi ngờ nội dung không an toàn/lừa đảo.");
                        response.put("scam_score", scamScore);

                        return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
                    }
                }
            }
            System.out.println(apiResponse.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("⚠️ Warning: Could not connect to Scam API. Proceeding with manual review.");
        }

        PendingJobPost jobPost = new PendingJobPost();
        jobPost.setTitle(postingRequest.getTitle());
        jobPost.setEmployer(jwtUtil.getEmployer(token));
        jobPost.setDescription(postingRequest.getDescription());
        jobPost.setWorkspacePicture(postingRequest.getWorkSpacePicture());
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

    public ResponseEntity<?> deletePendingPost(String token, long pendingId) {
        if(jwtUtil.checkPermission(token, "Admin") || jwtUtil.checkPermission(token, "Applicant")){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền truy cập");
        }
        PendingJobPost pendingJobPost = pendingJobPostRepository.findById(pendingId).orElse(null);
        if(pendingJobPost == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại");
        }
        pendingJobPostRepository.delete(pendingJobPost);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đã xóa bài viết thành công với id là: "+pendingId);
        response.put("status", "Success");
        return ResponseEntity.ok(response);
    }


    public ResponseEntity<?> acceptPost(String token, long pendingId){
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
        Map<String, Object> response = new HashMap<>();
        response.put("jobPostId", jobPost.getId());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> rejectPost(String token, long pendingId){
        if(!jwtUtil.checkPermission(token, "Admin"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền truy cập");
        PendingJobPost pendingJobPost= pendingJobPostRepository.findById(pendingId).orElse(null);
        if(pendingJobPost == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại");
        }
        pendingJobPostRepository.delete(pendingJobPost);
        Notification notification = new Notification();
        notification.setUser(pendingJobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng của bạn đã bị xóa do không tuân theo các quy định về nội dung: " + pendingJobPost.getTitle());
        notification.setRead(false);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Bài đăng đã bị xóa thành công");
    }

    public ResponseEntity<?> getSomePendingPosts(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
//        if (!jwtUtil.checkPermission(token, "Admin")) {
//            response.put("status", "fail");
//            response.put("message", "bạn không có quyền truy cập");
//            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
//        }
//        if (!jwtUtil.checkPermission(token, "Employer")) {
//            response.put("status", "fail");
//            response.put("message", "bạn không có quyền truy cập");
//            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
//        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PendingJobPost> pendingJobPostsPage = pendingJobPostRepository.findAll(pageable);

        List<Map<String, Object>> posts = pendingJobPostsPage.getContent().stream().map(pendingJobPost -> {
            String employerName = pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer().getFullName() : "Unknown";
            List<ImageFolders> folder = List.of();
            List<String> pics = new java.util.ArrayList<>(List.of());
            if (pendingJobPost.getWorkspacePicture() != null) {
                folder = imageFoldersRepository.findByFolderName(pendingJobPost.getWorkspacePicture());
            }
            for (ImageFolders imageFolder : folder) {
                if (imageFolder.getFolderName().equals(pendingJobPost.getWorkspacePicture())) {
                    pics.add(imageFolder.getFileName());
                }
            }
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", pendingJobPost.getId());
            postData.put("title", pendingJobPost.getTitle());
            postData.put("employerName", employerName);
            postData.put("employer", pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer() : null);
            postData.put("userId", pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer().getUser().getId() : null);
            postData.put("description", pendingJobPost.getDescription());
            postData.put("avatar", pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer().getUser().getAvatarUrl() : null);
            postData.put("workspacePicture", pics.toArray());
            postData.put("createdAt", pendingJobPost.getCreatedAt());
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng chờ duyệt thành công");
        response.put("posts", posts);
        response.put("totalPages", pendingJobPostsPage.getTotalPages());
        response.put("currentPage", pendingJobPostsPage.getNumber());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
