package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Post.PostingRequest;
import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.models.Enum.JobType;
import com.example.JFS_Job_Finding_Service.repository.ImageFoldersRepository;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import com.example.JFS_Job_Finding_Service.repository.PendingJobPostRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

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
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private ObjectMapper objectMapper;

    // Helper to format salary
    private String formatSalary(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return min.toPlainString() + " - " + max.toPlainString();
        }
        return "Thương lượng";
    }

    public ResponseEntity<?> addPost(String token, PostingRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Truy cập trái phép."));
        }
        if (!jwtUtil.checkWhetherIsEmployer(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Bạn không có quyền thực hiện thao tác này."));
        }

        Employer employer = jwtUtil.getEmployer(token);
        if (employer == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "Vui lòng đăng nhập trước khi tiếp tục!"));
        }

        switch (employer.getStatus()) {
            case PENDING -> {
                return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản đang chờ duyệt. Vui lòng kiên nhẫn."));
            }
            case REJECTED -> {
                return ResponseEntity.badRequest().body(Map.of("message", "Yêu cầu đã bị từ chối. Vui lòng cập nhật lại hồ sơ doanh nghiệp."));
            }
            case BANNED -> {
                return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản đã bị khóa. Vui lòng liên hệ bộ phận hỗ trợ."));
            }
        }

        // Variable declarations for parsed data
        Map<String, Object> descriptionMap;
        Map<String, Object> requirementsMap;
        Map<String, Object> responsibilitiesMap;
        Map<String, Object> advantagesMap;
        Map<String, Object> extensionMap = new HashMap<>(); // Default empty if null
        String workspacePictureFolder = null;

        try {
            // Validate required array fields
            if (request.getPositions() == null || request.getPositions().length == 0) {
                return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "Vui lòng chỉ định các vị trí tuyển dụng."));
            }
            if (request.getAddresses() == null || request.getAddresses().length == 0) {
                return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "Vui lòng nhập ít nhất một địa chỉ làm việc."));
            }

            // Parse JSON String fields to Maps
            descriptionMap = objectMapper.readValue(request.getDescription(), new TypeReference<>() {});
            requirementsMap = objectMapper.readValue(request.getRequirements(), new TypeReference<>() {});
            responsibilitiesMap = objectMapper.readValue(request.getResponsibilities(), new TypeReference<>() {});
            advantagesMap = objectMapper.readValue(request.getAdvantages(), new TypeReference<>() {});

            if (request.getExtension() != null && !request.getExtension().isEmpty()) {
                extensionMap = objectMapper.readValue(request.getExtension(), new TypeReference<>() {});
            }

            // Handle file upload
            if (request.getFiles() != null && request.getFiles().length > 0) {
                String folderNameKey = request.getTitle().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
                workspacePictureFolder = cloudinaryService.uploadFiles(request.getFiles(), folderNameKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "fail", "message", "Lỗi khi xử lý dữ liệu JSON hoặc tải ảnh: " + e.getMessage()));
        }

        // Scam detection logic (Keep existing logic on Title + Description)
        try {
            String rawDesc = descriptionMap.toString();
            String processedDesc = Arrays.stream(rawDesc.replace("{", "").replace("}", "").split(", "))
                    .map(part -> {
                        int idx = part.indexOf("=");
                        return (idx != -1) ? part.substring(idx + 1).trim() : part.trim();
                    })
                    .collect(Collectors.joining(". "));

            String textToCheck = request.getTitle() + " . " + processedDesc;
            String apiUrl = "http://host.docker.internal:8000/predict";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", textToCheck);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(apiUrl, requestBody, Map.class);

            if (apiResponse.getBody() != null && apiResponse.getBody().containsKey("scores")) {
                Map<String, Double> scores = (Map<String, Double>) apiResponse.getBody().get("scores");
                Double scamScore = scores.get("scam");

                if (scamScore > 0.7) {
                    response.put("status", "fail");
                    response.put("message", "Bài đăng bị hệ thống chặn do nghi ngờ chứa nội dung lừa đảo.");
                    response.put("scam_score", scamScore);
                    return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Cảnh báo: bot kiểm tra lừa đảo không phản hồi. Đang tiếp tục xử lý.");
        }

        // Create and Save PendingJobPost
        try {
            PendingJobPost jobPost = new PendingJobPost();
            jobPost.setTitle(request.getTitle());
            jobPost.setEmployer(employer);
            jobPost.setPositions(request.getPositions());

            // Set Mapped JSONB fields
            jobPost.setJobDescription(descriptionMap);
            jobPost.setRequirements(requirementsMap);
            jobPost.setResponsibilities(responsibilitiesMap);
            jobPost.setAdvantages(advantagesMap);
            jobPost.setExtension(extensionMap);

            // Set Simple fields
            jobPost.setAddresses(request.getAddresses());

            // Set Salary (Check logic handled by DB constraint, but safe to set null if needed)
            jobPost.setSalaryMin(request.getSalaryMin());
            jobPost.setSalaryMax(request.getSalaryMax());

            // Set Enum Type
            try {
                jobPost.setType(JobType.valueOf(request.getType()));
            } catch (IllegalArgumentException | NullPointerException e) {
                return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "Loại công việc không hợp lệ (FULL_TIME, PART_TIME, etc)."));
            }

            jobPost.setWorkspacePicture(workspacePictureFolder);

            pendingJobPostRepository.save(jobPost);

            Notification notification = new Notification();
            notification.setUser(employer.getUser());
            notification.setMessage("Bài đăng của bạn đã được gửi để duyệt: " + jobPost.getTitle());
            notification.setRead(false);
            notificationRepository.save(notification);

            response.put("status", "success");
            response.put("message", "Đã tạo bài đăng thành công, đang chờ quản trị viên phê duyệt!");
            response.put("jobPostId", jobPost.getId());

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "fail", "message", "Lỗi server khi lưu bài đăng: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> deletePendingPost(String token, long pendingId) {
        if(jwtUtil.checkPermission(token, "Admin") || jwtUtil.checkPermission(token, "Applicant")){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền thực hiện thao tác này.");
        }
        PendingJobPost pendingJobPost = pendingJobPostRepository.findById(pendingId).orElse(null);
        if(pendingJobPost == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại.");
        }
        pendingJobPostRepository.delete(pendingJobPost);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đã xóa bài đăng chờ duyệt thành công. ID: " + pendingId);
        response.put("status", "Success");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> acceptPost(String token, long pendingId){
        if(!jwtUtil.checkPermission(token, "Admin"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền phê duyệt bài đăng.");
        PendingJobPost pendingJobPost= pendingJobPostRepository.findById(pendingId).orElse(null);
        if(pendingJobPost == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại.");
        }
        JobPost jobPost = new JobPost();
        jobPost.setTitle(pendingJobPost.getTitle());
        jobPost.setEmployer(pendingJobPost.getEmployer());
        jobPost.setJobDescription(pendingJobPost.getJobDescription());
        jobPost.setRequirements(pendingJobPost.getRequirements());
        jobPost.setResponsibilities(pendingJobPost.getResponsibilities());
        jobPost.setAdvantages(pendingJobPost.getAdvantages());
        jobPost.setExtension(pendingJobPost.getExtension());
        jobPost.setType(pendingJobPost.getType());
        jobPost.setAddresses(pendingJobPost.getAddresses());
        jobPost.setSalaryMin(pendingJobPost.getSalaryMin());
        jobPost.setSalaryMax(pendingJobPost.getSalaryMax());
        jobPost.setWorkspacePicture(pendingJobPost.getWorkspacePicture());
        jobPost.setCreatedAt(pendingJobPost.getCreatedAt());
        jobPost.setPositions(pendingJobPost.getPositions());
        jobPostRepository.save(jobPost);
        pendingJobPostRepository.delete(pendingJobPost);
        Notification notification = new Notification();
        notification.setUser(pendingJobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng của bạn đã được phê duyệt và hiển thị: " + jobPost.getTitle());
        notification.setRead(false);
        notificationRepository.save(notification);
        Map<String, Object> response = new HashMap<>();
        response.put("jobPostId", jobPost.getId());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> rejectPost(String token, long pendingId){
        if(!jwtUtil.checkPermission(token, "Admin"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền thực hiện thao tác này.");
        PendingJobPost pendingJobPost= pendingJobPostRepository.findById(pendingId).orElse(null);
        if(pendingJobPost == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại.");
        }
        pendingJobPostRepository.delete(pendingJobPost);
        Notification notification = new Notification();
        notification.setUser(pendingJobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng của bạn đã bị từ chối do không tuân thủ quy định nội dung: " + pendingJobPost.getTitle());
        notification.setRead(false);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Đã từ chối bài đăng thành công.");
    }

    public ResponseEntity<?> getSomePendingPosts(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền truy cập.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        String[] roles={"Admin","Employer"};
        if (!jwtUtil.checkPermission(token, roles)) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền thực hiện chức năng này.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PendingJobPost> pendingJobPostsPage = pendingJobPostRepository.findAll(pageable);

        // Map to simplified structure
        List<Map<String, Object>> posts = pendingJobPostsPage.getContent().stream().map(pendingJobPost -> {
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", pendingJobPost.getId());
            postData.put("title", pendingJobPost.getTitle());
            postData.put("orgName", pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer().getOrgName() : "Unknown");
            postData.put("salary", formatSalary(pendingJobPost.getSalaryMin(), pendingJobPost.getSalaryMax()));
            postData.put("type", pendingJobPost.getType());
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng chờ duyệt thành công.");
        response.put("posts", posts);
        response.put("totalPages", pendingJobPostsPage.getTotalPages());
        response.put("currentPage", pendingJobPostsPage.getNumber());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> getPendingPostsForEmployer(String token, int page, int size) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return new ResponseEntity<>(Map.of("status", "fail", "message", "Truy cập trái phép."), HttpStatus.UNAUTHORIZED);
        }

        Employer employer = jwtUtil.getEmployer(token);
        if (employer == null) {
            return new ResponseEntity<>(Map.of("status", "fail", "message", "Bạn không phải là nhà tuyển dụng."), HttpStatus.FORBIDDEN);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Assumes pendingJobPostRepository has method: findByEmployer(Employer employer, Pageable pageable)
        Page<PendingJobPost> pendingJobPostsPage = pendingJobPostRepository.findByEmployer(employer, pageable);

        List<Map<String, Object>> posts = pendingJobPostsPage.getContent().stream().map(pendingJobPost -> {
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", pendingJobPost.getId());
            postData.put("title", pendingJobPost.getTitle());
            postData.put("orgName", employer.getOrgName());
            postData.put("salary", formatSalary(pendingJobPost.getSalaryMin(), pendingJobPost.getSalaryMax()));
            postData.put("type", pendingJobPost.getType());
            postData.put("createdAt", pendingJobPost.getCreatedAt());
            return postData;
        }).toList();

        response.put("status", "success");
        response.put("message", "Lấy danh sách bài đăng chờ duyệt thành công.");
        response.put("posts", posts);
        response.put("totalPages", pendingJobPostsPage.getTotalPages());
        response.put("currentPage", pendingJobPostsPage.getNumber());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    public ResponseEntity<?> getPendingJobPostDetail(String token, long pendingId) {
        Map<String, Object> response = new HashMap<>();

        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return new ResponseEntity<>(Map.of("status", "fail", "message", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        PendingJobPost pendingJobPost = pendingJobPostRepository.findById(pendingId).orElse(null);
        if (pendingJobPost == null) {
            response.put("status", "fail");
            response.put("message", "Bài đăng chờ duyệt không tồn tại.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        try {
            String employerName = pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer().getOrgName() : "Unknown";

            // Image handling
            List<ImageFolders> folder = List.of();
            List<String> pics = new ArrayList<>();
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
            postData.put("employerId", pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer().getId() : null);
            postData.put("description", pendingJobPost.getJobDescription());
            postData.put("requirements", pendingJobPost.getRequirements());
            postData.put("responsibilities", pendingJobPost.getResponsibilities());
            postData.put("advantages", pendingJobPost.getAdvantages());
            postData.put("extension", pendingJobPost.getExtension());
            postData.put("type", pendingJobPost.getType());
            postData.put("addresses", pendingJobPost.getAddresses());
            postData.put("positions", pendingJobPost.getPositions());
            postData.put("salaryMin", pendingJobPost.getSalaryMin());
            postData.put("salaryMax", pendingJobPost.getSalaryMax());
            postData.put("avatar", pendingJobPost.getEmployer() != null ? pendingJobPost.getEmployer().getUser().getAvatarUrl() : null);
            postData.put("workspacePicture", pics.toArray());
            postData.put("createdAt", pendingJobPost.getCreatedAt());

            response.put("status", "success");
            response.put("post", postData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("status", "error", "message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}