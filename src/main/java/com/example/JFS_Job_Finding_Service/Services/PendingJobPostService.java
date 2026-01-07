package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Post.JobPostSummaryDTO;
import com.example.JFS_Job_Finding_Service.DTO.Post.PostingRequest;
import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.models.Enum.JobType;
import com.example.JFS_Job_Finding_Service.models.Enum.VerificationStatus;
import com.example.JFS_Job_Finding_Service.models.POJO.JobPosition;
import com.example.JFS_Job_Finding_Service.repository.ImageFoldersRepository;
import com.example.JFS_Job_Finding_Service.repository.JobPostRepository;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import com.example.JFS_Job_Finding_Service.repository.PendingJobPostRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PendingJobPostService {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PendingJobPostRepository pendingJobPostRepository;
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

    private String formatSalary(BigDecimal min, BigDecimal max) {
        return (min != null && max != null) ? min.toPlainString() + " - " + max.toPlainString() : "Thương lượng";
    }

    private Employer validateEmployerAccess(String token) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) return null;
        if (!jwtUtil.checkWhetherIsEmployer(token)) return null;
        return jwtUtil.getEmployer(token);
    }

    private Map<String, Object> parseJobData(PostingRequest request) throws JsonProcessingException {
        Map<String, Object> parsedData = new HashMap<>();

        parsedData.put("description", objectMapper.readValue(request.getDescription(), new TypeReference<Map<String, Object>>() {}));
        parsedData.put("requirements", objectMapper.readValue(request.getRequirements(), new TypeReference<Map<String, Object>>() {}));
        parsedData.put("responsibilities", objectMapper.readValue(request.getResponsibilities(), new TypeReference<Map<String, Object>>() {}));
        parsedData.put("advantages", objectMapper.readValue(request.getAdvantages(), new TypeReference<Map<String, Object>>() {}));

        List<JobPosition> positionList = objectMapper.readValue(
                request.getPositions(),
                new TypeReference<List<JobPosition>>() {}
        );
        parsedData.put("positions", positionList);

        Map<String, Object> extensionMap = new HashMap<>();
        if (request.getExtension() != null && !request.getExtension().isEmpty()) {
            extensionMap = objectMapper.readValue(request.getExtension(), new TypeReference<Map<String, Object>>() {});
        }
        parsedData.put("extension", extensionMap);

        return parsedData;
    }

    private Double performScamCheck(String title, Map<String, Object> descriptionMap) {
        try {
            String rawDesc = descriptionMap.toString();
            String processedDesc = Arrays.stream(rawDesc.replace("{", "").replace("}", "").split(", "))
                    .map(part -> {
                        int idx = part.indexOf("=");
                        return (idx != -1) ? part.substring(idx + 1).trim() : part.trim();
                    })
                    .collect(Collectors.joining(". "));

            String textToCheck = title + " . " + processedDesc;
            String apiUrl = "http://host.docker.internal:8000/predict";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", textToCheck);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(apiUrl, requestBody, Map.class);

            if (apiResponse.getBody() != null && apiResponse.getBody().containsKey("scores")) {
                Map<String, Double> scores = (Map<String, Double>) apiResponse.getBody().get("scores");
                return scores.get("scam");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Scam detection service unavailable.");
        }
        return 0.0;
    }

    @SuppressWarnings("unchecked")
    public ResponseEntity<?> addPost(String token, PostingRequest request) {
        Employer employer = validateEmployerAccess(token);
        if (employer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Truy cập bị từ chối."));
        }

        if (employer.getStatus() != VerificationStatus.VERIFIED) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản của bạn chưa sẵn sàng (Pending/Rejected/Banned)."));
        }

        try {
            if (request.getPositions() == null || request.getPositions().isEmpty() || request.getAddresses() == null || request.getAddresses().length == 0) {
                return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "Thiếu thông tin vị trí hoặc địa chỉ."));
            }

            Map<String, Object> jsonData = parseJobData(request);

            Double scamScore = performScamCheck(request.getTitle(), (Map<String, Object>) jsonData.get("description"));
            if (scamScore > 0.7) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("status", "fail", "message", "Nội dung nghi ngờ lừa đảo.", "scam_score", scamScore));
            }

            String workspacePictureFolder = null;
            if (request.getFiles() != null && request.getFiles().length > 0) {
                String folderNameKey = request.getTitle().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
                workspacePictureFolder = cloudinaryService.uploadFiles(request.getFiles(), folderNameKey);
            }

            PendingJobPost jobPost = new PendingJobPost();
            jobPost.setTitle(request.getTitle());
            jobPost.setEmployer(employer);
            jobPost.setPositions((List<JobPosition>) jsonData.get("positions"));
            jobPost.setAddresses(request.getAddresses());
            jobPost.setSalaryMin(request.getSalaryMin());
            jobPost.setSalaryMax(request.getSalaryMax());
            jobPost.setType(JobType.valueOf(request.getType()));
            jobPost.setWorkspacePicture(workspacePictureFolder);

            jobPost.setJobDescription((Map<String, Object>) jsonData.get("description"));
            jobPost.setRequirements((Map<String, Object>) jsonData.get("requirements"));
            jobPost.setResponsibilities((Map<String, Object>) jsonData.get("responsibilities"));
            jobPost.setAdvantages((Map<String, Object>) jsonData.get("advantages"));
            jobPost.setExtension((Map<String, Object>) jsonData.get("extension"));

            pendingJobPostRepository.save(jobPost);

            Notification notification = new Notification();
            notification.setUser(employer.getUser());
            notification.setMessage("Bài đăng đã gửi duyệt: " + jobPost.getTitle());
            notification.setRead(false);
            notificationRepository.save(notification);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "message", "Đã gửi bài đăng chờ duyệt.", "jobPostId", jobPost.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "fail", "message", "Lỗi xử lý: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    public ResponseEntity<?> updatePendingPost(String token, long pendingId, PostingRequest request) {
        Employer employer = validateEmployerAccess(token);
        if (employer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Truy cập bị từ chối."));
        }

        PendingJobPost pendingPost = pendingJobPostRepository.findById(pendingId).orElse(null);
        if (pendingPost == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "fail", "message", "Bài đăng không tồn tại."));
        }

        if (!pendingPost.getEmployer().getId().equals(employer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Bạn không sở hữu bài đăng này."));
        }

        try {
            Map<String, Object> jsonData = parseJobData(request);

            Double scamScore = performScamCheck(request.getTitle(), (Map<String, Object>) jsonData.get("description"));
            if (scamScore > 0.7) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("status", "fail", "message", "Nội dung cập nhật nghi ngờ lừa đảo.", "scam_score", scamScore));
            }

            if (request.getFiles() != null && request.getFiles().length > 0) {
                String folderNameKey = request.getTitle().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
                String newPics = cloudinaryService.uploadFiles(request.getFiles(), folderNameKey);
                pendingPost.setWorkspacePicture(newPics);
            }

            pendingPost.setTitle(request.getTitle());
            pendingPost.setPositions((List<JobPosition>) jsonData.get("positions"));
            pendingPost.setAddresses(request.getAddresses());
            pendingPost.setSalaryMin(request.getSalaryMin());
            pendingPost.setSalaryMax(request.getSalaryMax());
            try {
                pendingPost.setType(JobType.valueOf(request.getType()));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "Loại công việc không hợp lệ."));
            }

            pendingPost.setJobDescription((Map<String, Object>) jsonData.get("description"));
            pendingPost.setRequirements((Map<String, Object>) jsonData.get("requirements"));
            pendingPost.setResponsibilities((Map<String, Object>) jsonData.get("responsibilities"));
            pendingPost.setAdvantages((Map<String, Object>) jsonData.get("advantages"));
            pendingPost.setExtension((Map<String, Object>) jsonData.get("extension"));

            pendingJobPostRepository.save(pendingPost);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Cập nhật thành công.", "id", pendingPost.getId()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "fail", "message", "Lỗi cập nhật: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> deletePendingPost(String token, long pendingId) {
        if (jwtUtil.checkPermission(token, "Admin") || jwtUtil.checkPermission(token, "Applicant")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền thực hiện thao tác này.");
        }
        if (pendingJobPostRepository.existsById(pendingId)) {
            pendingJobPostRepository.deleteById(pendingId);
            return ResponseEntity.ok(Map.of("status", "Success", "message", "Đã xóa bài đăng chờ duyệt."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại.");
    }

    public ResponseEntity<?> acceptPost(String token, long pendingId) {
        if (!jwtUtil.checkPermission(token, "Admin"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền phê duyệt.");

        PendingJobPost pending = pendingJobPostRepository.findById(pendingId).orElse(null);
        if (pending == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại.");

        JobPost jobPost = new JobPost();
        jobPost.setTitle(pending.getTitle());
        jobPost.setEmployer(pending.getEmployer());
        jobPost.setJobDescription(pending.getJobDescription());
        jobPost.setRequirements(pending.getRequirements());
        jobPost.setResponsibilities(pending.getResponsibilities());
        jobPost.setAdvantages(pending.getAdvantages());
        jobPost.setExtension(pending.getExtension());
        jobPost.setType(pending.getType());
        jobPost.setAddresses(pending.getAddresses());
        jobPost.setSalaryMin(pending.getSalaryMin());
        jobPost.setSalaryMax(pending.getSalaryMax());
        jobPost.setWorkspacePicture(pending.getWorkspacePicture());
        jobPost.setCreatedAt(pending.getCreatedAt());
        jobPost.setPositions(pending.getPositions());

        jobPostRepository.save(jobPost);
        pendingJobPostRepository.delete(pending);

        Notification n = new Notification();
        n.setUser(pending.getEmployer().getUser());
        n.setMessage("Bài đăng đã được duyệt: " + jobPost.getTitle());
        n.setRead(false);
        notificationRepository.save(n);

        return ResponseEntity.ok(Map.of("status", "success", "jobPostId", jobPost.getId()));
    }

    public ResponseEntity<?> rejectPost(String token, long pendingId) {
        if (!jwtUtil.checkPermission(token, "Admin"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền từ chối.");

        PendingJobPost pending = pendingJobPostRepository.findById(pendingId).orElse(null);
        if (pending == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bài đăng không tồn tại.");

        pendingJobPostRepository.delete(pending);
        Notification n = new Notification();
        n.setUser(pending.getEmployer().getUser());
        n.setMessage("Bài đăng bị từ chối: " + pending.getTitle());
        n.setRead(false);
        notificationRepository.save(n);

        return ResponseEntity.ok("Đã từ chối bài đăng.");
    }

    public ResponseEntity<?> getSomePendingPosts(String token, int page, int size) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token)) || !jwtUtil.checkPermission(token, new String[]{"Admin", "Employer"})) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Truy cập bị từ chối."));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PendingJobPost> pageResult = pendingJobPostRepository.findAll(pageable);

        return ResponseEntity.ok(createListResponse(pageResult));
    }

    public ResponseEntity<?> getPendingPostsForEmployer(String token, int page, int size) {
        Employer employer = validateEmployerAccess(token);
        if (employer == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Truy cập bị từ chối."));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PendingJobPost> pageResult = pendingJobPostRepository.findByEmployer(employer, pageable);

        return ResponseEntity.ok(createListResponse(pageResult));
    }

    private Map<String, Object> createListResponse(Page<PendingJobPost> pageResult) {
        List<JobPostSummaryDTO> posts = pageResult.getContent().stream().map(p -> JobPostSummaryDTO.builder()
                .id(String.valueOf(p.getId()))
                .title(p.getTitle())
                .employerName(p.getEmployer().getUser().getFullName())
                .employerUserId(p.getEmployer().getUser().getId())
                .employerId(p.getEmployer().getId())
                .avatarUrl(p.getEmployer().getUser().getAvatarUrl())
                .orgName(p.getEmployer() != null ? p.getEmployer().getOrgName() : "Unknown")
                .jobType(String.valueOf(p.getType()))
                .salary(formatSalary(p.getSalaryMin(), p.getSalaryMax()))
                .createdAt(p.getCreatedAt())
                .build()).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("posts", posts);
        response.put("totalPages", pageResult.getTotalPages());
        response.put("currentPage", pageResult.getNumber());
        return response;
    }

    public ResponseEntity<?> getPendingJobPostDetail(String token, long pendingId) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }

        PendingJobPost pending = pendingJobPostRepository.findById(pendingId).orElse(null);
        if (pending == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "fail", "message", "Không tìm thấy bài đăng."));

        try {
            List<String> pics = new ArrayList<>();
            if (pending.getWorkspacePicture() != null) {
                imageFoldersRepository.findByFolderName(pending.getWorkspacePicture())
                        .stream()
                        .filter(img -> img.getFolderName().equals(pending.getWorkspacePicture()))
                        .forEach(img -> pics.add(img.getFileName()));
            }

            Map<String, Object> postData = new HashMap<>();
            postData.put("id", pending.getId());
            postData.put("title", pending.getTitle());
            postData.put("employerName", pending.getEmployer() != null ? pending.getEmployer().getOrgName() : "Unknown");
            postData.put("employerId", pending.getEmployer() != null ? pending.getEmployer().getId() : null);
            postData.put("employerUserId", pending.getEmployer().getUser().getId());
            postData.put("description", pending.getJobDescription());
            postData.put("requirements", pending.getRequirements());
            postData.put("responsibilities", pending.getResponsibilities());
            postData.put("advantages", pending.getAdvantages());
            postData.put("extension", pending.getExtension());
            postData.put("type", pending.getType());
            postData.put("addresses", pending.getAddresses());
            postData.put("positions", pending.getPositions());
            postData.put("salaryMin", pending.getSalaryMin());
            postData.put("salaryMax", pending.getSalaryMax());
            postData.put("avatar", pending.getEmployer() != null ? pending.getEmployer().getUser().getAvatarUrl() : null);
            postData.put("workspacePicture", pics);
            postData.put("createdAt", pending.getCreatedAt());

            return ResponseEntity.ok(Map.of("status", "success", "post", postData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}