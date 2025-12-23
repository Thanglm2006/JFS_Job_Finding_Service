package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Post.*;
import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.models.Enum.JobType;
import com.example.JFS_Job_Finding_Service.models.Enum.PositionStatus;
import com.example.JFS_Job_Finding_Service.models.POJO.JobPosition;
import com.example.JFS_Job_Finding_Service.repository.*;
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
public class PostService {
    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ImageFoldersRepository imageFoldersRepository;
    @Autowired
    private SavedJobRepository savedJobRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CloudinaryService cloudinaryService;

    // --- Helpers ---

    private String formatSalary(BigDecimal min, BigDecimal max) {
        return (min != null && max != null) ? min.toPlainString() + " - " + max.toPlainString() : "Thương lượng";
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

    // Reuse scam check logic for updates to prevent malicious edits
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
            // Ignore if service down
        }
        return 0.0;
    }

    private JobPostSummaryDTO mapToSummaryDTO(JobPost jobPost, Applicant applicant) {
        boolean isSaved = false;
        boolean isApplied = false;
        if (applicant != null) {
            isSaved = !savedJobRepository.findByApplicantAndJob(applicant, jobPost).isEmpty();
            isApplied = applicationRepository.findByApplicant(applicant)
                    .stream().anyMatch(app -> app.getJob().getId().equals(jobPost.getId()));
        }
        return JobPostSummaryDTO.builder()
                .id(jobPost.getId())
                .title(jobPost.getTitle())
                .employerName(jobPost.getEmployer().getUser().getFullName())
                .employerUserId(jobPost.getEmployer().getUser().getId())
                .employerId(jobPost.getEmployer().getId())
                .orgName(jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown")
                .jobType(String.valueOf(jobPost.getType()))
                .salary(formatSalary(jobPost.getSalaryMin(), jobPost.getSalaryMax()))
                .createdAt(jobPost.getCreatedAt())
                .isSaved(isSaved)
                .isApplied(isApplied)
                .build();
    }
    private JobPostSummaryDTOForEmployer mapToPostReturnEmployer(JobPost jobPost, Applicant applicant) {
        boolean isSaved = false;
        boolean isApplied = false;
        if (applicant != null) {
            isSaved = !savedJobRepository.findByApplicantAndJob(applicant, jobPost).isEmpty();
            isApplied = applicationRepository.findByApplicant(applicant)
                    .stream().anyMatch(app -> app.getJob().getId().equals(jobPost.getId()));
        }
        List<Application> applicationsL = applicationRepository.findByJob(jobPost);
        applicationsL.sort(Comparator.comparing(Application::getAppliedAt).reversed());
        List<Map<String, Object>> applications= new ArrayList<>();
        for(Application app : applicationsL) {
            Map<String, Object> application = new HashMap<>();
            application.put("id", app.getId());
            application.put("cv", app.getCv());
            application.put("applicantId", app.getApplicant().getId());
            application.put("userId", app.getApplicant().getUser().getId());
            application.put("position",app.getPosition());
            applications.add(application);
        }
        return JobPostSummaryDTOForEmployer.builder()
                .id(jobPost.getId())
                .title(jobPost.getTitle())
                .employerName(jobPost.getEmployer().getUser().getFullName())
                .employerUserId(jobPost.getEmployer().getUser().getId())
                .employerId(jobPost.getEmployer().getId())
                .orgName(jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown")
                .jobType(String.valueOf(jobPost.getType()))
                .salary(formatSalary(jobPost.getSalaryMin(), jobPost.getSalaryMax()))
                .createdAt(jobPost.getCreatedAt())
                .isSaved(isSaved)
                .isApplied(isApplied)
                .applicants(applications)
                .build();
    }

    // --- Main Methods ---

    public ResponseEntity<?> getSomePosts(String token, int page, int size) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }
        Applicant applicant = jwtUtil.getApplicant(token);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobPost> jobPostsPage = jobPostRepository.findAll(pageable);

        List<JobPostSummaryDTO> posts = jobPostsPage.getContent().stream().filter(post -> post.getPositions() != null &&
                        post.getPositions().stream()
                                .anyMatch(pos -> PositionStatus.OPEN.equals(pos.getStatus())))
                .map(post -> mapToSummaryDTO(post, applicant))
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("posts", posts);
        response.put("totalPages", jobPostsPage.getTotalPages());
        response.put("currentPage", jobPostsPage.getNumber());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> fullTextSearchPosts(String token, JobSearchRequest searchDTO) {
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Not an applicant"));
        }

        if (searchDTO.getType() != null && searchDTO.getType().isEmpty()) searchDTO.setType(null);

        List<JobPost> jobPosts = jobPostRepository.searchWithPGroonga(
                searchDTO.getKeyword(), searchDTO.getType(), searchDTO.getAddress(),
                searchDTO.getMinSalary(), searchDTO.getMaxSalary(),
                searchDTO.getLimit(), searchDTO.getOffset()
        );

        List<JobPostSummaryDTO> posts = jobPosts.stream().filter(post -> post.getPositions() != null &&
                        post.getPositions().stream()
                                .anyMatch(pos -> PositionStatus.OPEN.equals(pos.getStatus())))
                .map(post -> mapToSummaryDTO(post, applicant))
                .toList();

        return ResponseEntity.ok(Map.of("status", "success", "posts", posts, "totalResults", posts.size()));
    }
    public ResponseEntity<?> findByEmployerName(String token, String name, int page, int limit) {
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Not an applicant"));
        }
        List<JobPost> jobPosts = jobPostRepository.findByEmployerName(name, page, limit);
        List<JobPostSummaryDTO> posts = jobPosts.stream().filter(post -> post.getPositions() != null &&
                        post.getPositions().stream()
                                .anyMatch(pos -> PositionStatus.OPEN.equals(pos.getStatus())))
                .map(post -> mapToSummaryDTO(post, applicant))
                .toList();
        return ResponseEntity.ok(Map.of("status", "success", "posts", posts, "totalResults", posts.size()));
    }
    public ResponseEntity<?> findByOrgName(String token, String name, int page, int limit) {
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Not an applicant"));
        }
        List<JobPost> jobPosts = jobPostRepository.findByOrgName(name, page, limit);
        List<JobPostSummaryDTO> posts = jobPosts.stream().filter(post -> post.getPositions() != null &&
                        post.getPositions().stream()
                                .anyMatch(pos -> PositionStatus.OPEN.equals(pos.getStatus())))
                .map(post -> mapToSummaryDTO(post, applicant))
                .toList();
        return ResponseEntity.ok(Map.of("status", "success", "posts", posts, "totalResults", posts.size()));
    }

    public ResponseEntity<?> getSomePostOfEmployer(String token, int page, int size) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail"));
        }
        Employer employer = jwtUtil.getEmployer(token);
        if (employer == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail"));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobPost> jobPostsPage = jobPostRepository.findByEmployer(employer, pageable);

        List<JobPostSummaryDTOForEmployer> posts = jobPostsPage.getContent().stream()
                .map(post -> mapToPostReturnEmployer(post, null))
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("posts", posts);
        response.put("totalPages", jobPostsPage.getTotalPages());
        response.put("currentPage", jobPostsPage.getNumber());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getJobPostDetail(String token, String postId) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }

        JobPost jobPost = jobPostRepository.findById(postId).orElse(null);
        if (jobPost == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "fail"));

        Applicant applicant = jwtUtil.getApplicant(token);
        try {
            boolean isSaved = false;
            boolean isApplied = false;
            if (applicant != null) {
                isSaved = !savedJobRepository.findByApplicantAndJob(applicant, jobPost).isEmpty();
                isApplied = applicationRepository.findByApplicant(applicant)
                        .stream().anyMatch(app -> app.getJob().getId().equals(jobPost.getId()));
            }

            List<String> workSpacePictures = new ArrayList<>();
            if (jobPost.getWorkspacePicture() != null) {
                imageFoldersRepository.findByFolderName(jobPost.getWorkspacePicture())
                        .stream().filter(img -> img.getFolderName().equals(jobPost.getWorkspacePicture()))
                        .forEach(img -> workSpacePictures.add(img.getFileName()));
            }

            JobPostDetailDTO postData = JobPostDetailDTO.builder()
                    .id(jobPost.getId())
                    .title(jobPost.getTitle())
                    .employerId(jobPost.getEmployer() != null ? jobPost.getEmployer().getId() : null)
                    .orgName(jobPost.getEmployer().getOrgName() != null ? jobPost.getEmployer().getOrgName() : jobPost.getEmployer().getUser().getFullName())
                    .addresses(jobPost.getAddresses())
                    .jobType(String.valueOf(jobPost.getType()))
                    .advantages(jobPost.getAdvantages())
                    .employerUserId(jobPost.getEmployer().getUser().getId())
                    .employerName(jobPost.getEmployer().getUser().getFullName())
                    .isSaved(isSaved)
                    .isApplied(isApplied)
                    .createdAt(jobPost.getCreatedAt())
                    .description(jobPost.getJobDescription())
                    .orgAvatar(jobPost.getEmployer().getUser().getAvatarUrl())
                    .positions(jobPost.getPositions())
                    .requirements(jobPost.getRequirements())
                    .extension(jobPost.getExtension())
                    .responsibilities(jobPost.getResponsibilities())
                    .salaryMin(jobPost.getSalaryMin())
                    .salaryMax(jobPost.getSalaryMax())
                    .workspacePictures(workSpacePictures)
                    .build();

            return ResponseEntity.ok(Map.of("status", "success", "post", postData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    public ResponseEntity<?> updatePost(String token, String postId, PostingRequest request) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }
        Employer currentEmployer = jwtUtil.getEmployer(token);
        if (currentEmployer == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Permission denied"));
        }

        JobPost jobPost = jobPostRepository.findById(postId).orElse(null);
        if (jobPost == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "fail", "message", "Post not found"));
        }
        if (!jobPost.getEmployer().getId().equals(currentEmployer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail", "message", "Permission denied"));
        }

        try {
            Map<String, Object> jsonData = parseJobData(request);

            // Re-check for scams on update
            Double scamScore = performScamCheck(request.getTitle(), (Map<String, Object>) jsonData.get("description"));
            if (scamScore > 0.7) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("status", "fail", "message", "Suspicious content detected"));
            }

            if (request.getFiles() != null && request.getFiles().length > 0) {
                String folderNameKey = request.getTitle().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
                String newPics = cloudinaryService.uploadFiles(request.getFiles(), folderNameKey);
                jobPost.setWorkspacePicture(newPics);
            }

            jobPost.setTitle(request.getTitle());
            jobPost.setPositions((List<JobPosition>) jsonData.get("positions"));
            jobPost.setAddresses(request.getAddresses());
            jobPost.setSalaryMin(request.getSalaryMin());
            jobPost.setSalaryMax(request.getSalaryMax());
            try {
                jobPost.setType(JobType.valueOf(request.getType()));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "Invalid Job Type"));
            }

            jobPost.setJobDescription((Map<String, Object>) jsonData.get("description"));
            jobPost.setRequirements((Map<String, Object>) jsonData.get("requirements"));
            jobPost.setResponsibilities((Map<String, Object>) jsonData.get("responsibilities"));
            jobPost.setAdvantages((Map<String, Object>) jsonData.get("advantages"));
            jobPost.setExtension((Map<String, Object>) jsonData.get("extension"));

            jobPostRepository.save(jobPost);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Post updated successfully", "postId", jobPost.getId()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "fail", "message", e.getMessage()));
        }
    }

    public ResponseEntity<?> deletePost(String token, String postId) {
        if (!jwtUtil.checkPermission(token, "Admin") && !jwtUtil.checkPermission(token, "Employer")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail", "message", "Unauthorized"));
        }
        JobPost jobPost = jobPostRepository.findById(postId).orElse(null);
        if (jobPost == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "fail"));

        jobPostRepository.delete(jobPost);
        savedJobRepository.deleteAll(savedJobRepository.findByJob(jobPost));

        Notification notification = new Notification();
        notification.setUser(jobPost.getEmployer().getUser());
        notification.setMessage("Bài đăng \"" + jobPost.getTitle() + "\" đã được xóa.");
        notification.setRead(false);
        notificationRepository.save(notification);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Deleted successfully"));
    }

    public ResponseEntity<?> getAppliedJobs(String token, int page, int size) {
        if (!tokenService.validateToken(token, jwtUtil.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "fail"));
        }
        Applicant applicant = jwtUtil.getApplicant(token);
        if (applicant == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "fail"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Application> applicationPage = applicationRepository.findByApplicant(applicant, pageable);

        List<Map<String, Object>> applications = applicationPage.getContent().stream().map(app -> {
            JobPost job = app.getJob();
            Map<String, Object> data = new HashMap<>();
            data.put("applicationId", app.getId());
            data.put("jobId", job.getId());
            data.put("jobTitle", job.getTitle());
            data.put("jobCreatedAt", job.getCreatedAt());
            data.put("applicantId", applicant.getId());
            data.put("jobEmployerName", job.getEmployer() != null ? job.getEmployer().getOrgName() : "Unknown");
            data.put("status", app.getStatus());
            data.put("appliedAt", app.getAppliedAt());
            data.put("employerAvatar", job.getEmployer() != null ? job.getEmployer().getUser().getAvatarUrl() : null);
            return data;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("applications", applications);
        response.put("totalPages", applicationPage.getTotalPages());
        response.put("currentPage", applicationPage.getNumber());
        response.put("totalApplications", applicationPage.getTotalElements());
        return ResponseEntity.ok(response);
    }
}