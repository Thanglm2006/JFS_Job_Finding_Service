package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Application.ApplicantRegisterRequest;
import com.example.JFS_Job_Finding_Service.DTO.Auth.*;
import com.example.JFS_Job_Finding_Service.DTO.Employer.EmployerRequestInfoDTO;
import com.example.JFS_Job_Finding_Service.DTO.Employer.EmployerUpdateDTO;
import com.example.JFS_Job_Finding_Service.DTO.Employer.ReviewEmployerDTO;
import com.example.JFS_Job_Finding_Service.DTO.Post.JobPostSummaryDTO;
import com.example.JFS_Job_Finding_Service.models.*;
import com.example.JFS_Job_Finding_Service.models.Enum.EmployerType;
import com.example.JFS_Job_Finding_Service.models.Enum.VerificationStatus;
import com.example.JFS_Job_Finding_Service.repository.*;
import com.example.JFS_Job_Finding_Service.security.PasswordConfig;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final ApplicantRepository applicantRepository;
    private final JwtUtil jwtUtil;
    private final CloudinaryService cloudinaryService;
    private final MailService mailService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder = PasswordConfig.passwordEncoder();
    private final S3Service s3Service;
    private final Map<String, PendingRegistration> pendingRegisterMap = new ConcurrentHashMap<>();
    private final Map<String, VerificationInfo> passwordResetMap = new ConcurrentHashMap<>();

    private static final int EXPIRATION_MINUTES = 5;
    private final NotificationRepository notificationRepository;
    private final EmployerRequestRepository employerRequestRepository;
    private final JobPostRepository jobPostRepository;
    private final SavedJobRepository savedJobRepository;
    private final ApplicationRepository applicationRepository;

    private String generateVerificationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return code.toString();
    }

    @Scheduled(fixedRate = 60000)
    public void removeExpiredRegistrations() {
        LocalDateTime now = LocalDateTime.now();
        pendingRegisterMap.entrySet().removeIf(entry ->
                entry.getValue().getExpiryTime().isBefore(now)
        );
        passwordResetMap.entrySet().removeIf(entry ->
                entry.getValue().isExpired()
        );
    }

    public User getUserById(long id) {
        return userRepository.findById(id).orElse(null);
    }


    public ResponseEntity<?> EmployerLogin(String email, String password) {
        return genericLogin(email, password, "Employer");
    }

    public ResponseEntity<?> ApplicantLogin(String email, String password) {
        return genericLogin(email, password, "Applicant");
    }

    private ResponseEntity<?> genericLogin(String email, String password, String requiredRole) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            response.put("error", "User not found");
            response.put("message", "Tài khoản không tồn tại trên hệ thống.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User user = userOpt.get();

        boolean roleValid = false;
        if ("Employer".equals(requiredRole)) {
            roleValid = employerRepository.findByUser(user).isPresent();
        } else if ("Applicant".equals(requiredRole)) {
            roleValid = applicantRepository.findByUser(user).isPresent();
        }

        if (!roleValid || !user.getRole().equalsIgnoreCase(requiredRole)) {
            response.put("error", "Wrong role");
            response.put("message", "Tài khoản không đúng vai trò truy cập hoặc không tồn tại.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            response.put("error", "Invalid password");
            response.put("message", "Mật khẩu không chính xác. Vui lòng thử lại.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!user.isActive()) {
            response.put("error", "User banned/inactive");
            response.put("message", "Tài khoản của bạn hiện đang bị khóa hoặc chưa được kích hoạt.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        response.put("status", "success");
        response.put("token", token);
        response.put("userId", user.getId());
        if(requiredRole.equalsIgnoreCase("Employer")) {
            Employer employer =  employerRepository.findByUser(user).get();
            response.put("employerId", employer.getId());
            response.put("status",employer.getStatus());
        }
        else{
            Applicant applicant =  applicantRepository.findByUser(user).get();
            response.put("applicantId", applicant.getId());
            response.put("resume", applicant.getResume());
        }
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("fullName", user.getFullName());
        response.put("avatarUrl", user.getAvatarUrl());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> EmployerRegister(EmployerRegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        ResponseEntity<?> validationError = validateRegistrationRequest(
                request.getEmail(), request.getPassword(), request.getRetypePass(),
                request.getName(), request.getDateOfBirth(), request.getGender()
        );
        if (validationError != null) return validationError;

        EmployerType typeEnum;
        String customType = null;
        try {
            typeEnum = EmployerType.valueOf(request.getEmployerType());
        } catch (IllegalArgumentException | NullPointerException ex) {
            typeEnum = EmployerType.Other;
            customType = request.getCustomType();
        }

        if (request.getOrg() == null || request.getOrg().trim().isEmpty()) {
            response.put("error", "Invalid org");
            response.put("message", "Tên tổ chức/doanh nghiệp không được để trống.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User user = createUserEntity(request.getEmail(), request.getPassword(), request.getName(),
                request.getDateOfBirth(), request.getGender(), "Employer");

        Employer employer = new Employer();
        employer.setUser(user);
        employer.setOrgName(request.getOrg());
        employer.setType(typeEnum);
        employer.setCustomType(customType);
        employer.setStatus(VerificationStatus.PENDING);

        return processPendingRegistration(user, employer, null);
    }

    // Using DTO for review
    public ResponseEntity<?> acceptEmployerRegistration(String token, ReviewEmployerDTO dto) {
        Map<String, Object> response = new HashMap<>();
        if(!jwtUtil.checkPermission(token,"admin") && !jwtUtil.validateToken(token)){
            response.put("error", "Invalid access");
            response.put("message", "Bạn không có quyền thực hiện thao tác phê duyệt này.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Employer employer =  employerRepository.findById(dto.getEmployerId()).orElse(null);
        if(employer == null) {
            response.put("message", "Không tìm thấy thông tin nhà tuyển dụng.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        if(employer.getStatus() == VerificationStatus.VERIFIED) {
            return ResponseEntity.ok().body(Map.of("message","đã phê duyệt người này trước đó rồi!"));
        }
        employer.setStatus(VerificationStatus.VERIFIED);
        employer.setVerifiedAt(LocalDateTime.now());
        employerRepository.save(employer);

        EmployerRequest request = employerRequestRepository.findById(dto.getRequestId()).orElse(null);
        if (request != null) {
            request.setStatus(VerificationStatus.VERIFIED);
            employerRequestRepository.save(request);
        }
        else{
            return ResponseEntity.badRequest().body(Map.of("message","not found request"));
        }

        Notification notification = new Notification();
        notification.setUser(employer.getUser());
        notification.setMessage("Chúc mừng! Tài khoản nhà tuyển dụng của bạn đã được phê duyệt. Bạn có thể đăng tin tuyển dụng ngay bây giờ.");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        return ResponseEntity.ok(Map.of("message", "Đã phê duyệt tài khoản nhà tuyển dụng thành công."));
    }

    // Using DTO for rejection
    public ResponseEntity<?> rejectEmployerRegistration(String token, ReviewEmployerDTO dto) {
        Map<String, Object> response = new HashMap<>();
        if(!jwtUtil.checkPermission(token,"admin") && !jwtUtil.validateToken(token)){
            response.put("error", "Invalid access");
            response.put("message", "Bạn không có quyền thực hiện thao tác từ chối này.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Employer employer =  employerRepository.findById(dto.getEmployerId()).orElse(null);
        if(employer == null) {
            response.put("message", "Không tìm thấy thông tin nhà tuyển dụng.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        if(employer.getStatus() == VerificationStatus.VERIFIED) {
            return ResponseEntity.ok().body(Map.of("message","đã phê duyệt người này trước đó rồi!"));
        }
        employer.setStatus(VerificationStatus.REJECTED);

        employerRepository.save(employer);

        EmployerRequest request = employerRequestRepository.findById(dto.getRequestId()).orElse(null);
        if (request != null) {
            request.setStatus(VerificationStatus.REJECTED);
            request.setRejectionReason(dto.getReason());
            employerRequestRepository.save(request);
        }

        Notification notification = new Notification();
        notification.setUser(employer.getUser());
        notification.setMessage("Rất tiếc, yêu cầu đăng ký của bạn đã bị từ chối. Lý do: " + dto.getReason());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        return ResponseEntity.ok(Map.of("message", "Đã từ chối yêu cầu đăng ký của nhà tuyển dụng."));
    }

    public ResponseEntity<?> ApplicantRegister(ApplicantRegisterRequest request) {
        ResponseEntity<?> validationError = validateRegistrationRequest(
                request.getEmail(), request.getPassword(), request.getPassword(),
                request.getName(), request.getDateOfBirth(), request.getGender()
        );
        if (validationError != null) return validationError;

        User user = createUserEntity(request.getEmail(), request.getPassword(), request.getName(),
                request.getDateOfBirth(), request.getGender(), "Applicant");

        Applicant applicant = Applicant.builder().user(user).build();

        return processPendingRegistration(user, null, applicant);
    }

    private ResponseEntity<?> processPendingRegistration(User user, Employer employer, Applicant applicant) {
        Map<String, Object> response = new HashMap<>();
        String email = user.getEmail();
        String code = generateVerificationCode();

        pendingRegisterMap.remove(email);

        PendingRegistration pendingRegistration = PendingRegistration.builder()
                .user(user)
                .employer(employer)
                .applicant(applicant)
                .verificationCode(code)
                .expiryTime(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .build();

        try {
            mailService.sendVerificationEmailHTML(email, code);
        } catch (Exception e) {
            log.error("Failed to send email to {}", email, e);
            response.put("error", "Mail send failed");
            response.put("message", "Hệ thống gặp sự cố khi gửi email xác nhận. Vui lòng thử lại sau.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        pendingRegisterMap.put(email, pendingRegistration);
        response.put("status", "success");
        response.put("message", "Mã xác nhận đã được gửi thành công. Vui lòng kiểm tra hộp thư email của bạn!");
        return ResponseEntity.ok(response);
    }

    private User createUserEntity(String email, String password, String name, LocalDate dob, String gender, String role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        if (name != null) {
            name = name.trim();
            name = Normalizer.normalize(name, Normalizer.Form.NFC);
        }
        user.setFullName(name);
        user.setDateOfBirth(dob);
        user.setGender(gender != null ? gender.toLowerCase() : "other");
        user.setRole(role);
        user.setActive(false);
        return user;
    }

    private ResponseEntity<?> validateRegistrationRequest(String email, String pass, String confirmPass,
                                                          String name, LocalDate dob, String gender) {
        Map<String, Object> response = new HashMap<>();

        if (!pass.equals(confirmPass)) {
            response.put("error", "Password mismatch");
            response.put("message", "Mật khẩu xác nhận không trùng khớp.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByEmail(email).isPresent()) {
            response.put("error", "Duplicate email");
            response.put("message", "Địa chỉ email này đã được sử dụng trên hệ thống.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (name == null || !name.matches("^[\\p{L} ]+$")) {
            response.put("error", "Invalid name format");
            response.put("message", "Họ tên không hợp lệ. Vui lòng chỉ sử dụng chữ cái và khoảng trắng.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (dob != null) {
            LocalDate birthDate = dob;
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            if (age < 15) {
                response.put("error", "Age violation");
                response.put("message", "Bạn phải từ đủ 15 tuổi trở lên để có thể đăng ký tài khoản.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } else {
            response.put("error", "Missing Date");
            response.put("message", "Vui lòng cung cấp đầy đủ thông tin ngày sinh.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String normalizedGender = (gender != null) ? gender.toLowerCase() : "";
        if (!List.of("male", "female", "other").contains(normalizedGender)) {
            response.put("error", "Invalid gender");
            response.put("message", "Giới tính không hợp lệ. Vui lòng chọn giới tính!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    public ResponseEntity<?> sendVerificationEmailHTML(String email) {
        Map<String, Object> response = new HashMap<>();
        PendingRegistration pendingReg = pendingRegisterMap.get(email);

        if (pendingReg == null) {
            response.put("error", "Invalid");
            response.put("message", "Yêu cầu đăng ký không tồn tại hoặc phiên làm việc đã hết hạn.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String code = generateVerificationCode();
        pendingReg.setVerificationCode(code);
        pendingReg.setExpiryTime(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));

        pendingRegisterMap.put(email, pendingReg);

        try {
            mailService.sendVerificationEmailHTML(email, code);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi hệ thống khi gửi email.");
        }
        response.put("status", "success");
        response.put("message", "Mã xác nhận mới đã được gửi tới email của bạn.");
        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> verifyEmail(VerifyEmailDTO verifyEmailDTO) {
        Map<String, Object> response = new HashMap<>();
        String email = verifyEmailDTO.getEmail();
        PendingRegistration pendingReg = pendingRegisterMap.get(email);

        if (pendingReg == null) {
            response.put("error", "Invalid request");
            response.put("message", "Yêu cầu xác thực không hợp lệ.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.now().isAfter(pendingReg.getExpiryTime())) {
            pendingRegisterMap.remove(email);
            response.put("error", "Expired");
            response.put("message", "Mã xác nhận của bạn đã hết hạn. Vui lòng yêu cầu mã mới.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!pendingReg.getVerificationCode().equalsIgnoreCase(verifyEmailDTO.getCode())) {
            response.put("error", "Wrong code");
            response.put("message", "Mã xác nhận không chính xác.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User user = pendingReg.getUser();
        user.setActive(true);
        User savedUser = userRepository.save(user);

        if (pendingReg.getApplicant() != null) {
            Applicant applicant = pendingReg.getApplicant();
            applicant.setUser(savedUser);
            applicantRepository.save(applicant);
        } else if (pendingReg.getEmployer() != null) {
            Employer employer = pendingReg.getEmployer();
            employer.setUser(savedUser);
            employerRepository.save(employer);
        }

        pendingRegisterMap.remove(email);
        response.put("status", "success");
        response.put("message", "Chúc mừng! Email của bạn đã được xác minh thành công.");
        return ResponseEntity.ok(response);
    }
    private Map<String,Object> getUserInfor(User user){
        Map<String, Object> response = new HashMap<>();
        response.put("phone", user.getPhone());
        response.put("address", user.getAddress());
        response.put("gender", user.getGender());
        response.put("date_of_birth", user.getDateOfBirth());
        response.put("avatarUrl", user.getAvatarUrl());
        response.put("createdAt", user.getCreatedAt());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole());
        return response;
    }
    public ResponseEntity<?> getEmployerProfile(String token, String employerId) {
        Map<String, Object> response = new HashMap<>();
        String email;
        if(token!=null&&!token.isEmpty())
        email = jwtUtil.extractEmail(token);
        else email=null;
        // 2. Lấy thông tin Employer
        Optional<Employer> employerOptional = employerRepository.findById(employerId);
        if (employerOptional.isEmpty()) {
            response.put("status", "fail");
            response.put("message", "Không tìm thấy thông tin người dùng.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        Employer employer = employerOptional.get();
        User currentUser;
        if(email!=null)
        currentUser = userRepository.findByEmail(email).orElse(null);
        else currentUser=null;
        Applicant currentApplicant = null;
        if (currentUser != null && "Applicant".equals(currentUser.getRole())) { // Check role nếu cần
            currentApplicant = applicantRepository.findByUserId(currentUser.getId()).orElse(null);
        }

        User user = employer.getUser();
        Map<String, Object> userInfor = getUserInfor(user);
        response.putAll(userInfor);

        response.put("status", "success");
        response.put("employerId", employerId);
        response.put("userId", employer.getUser().getId());
        response.put("organization", employer.getOrgName());
        response.put("field", employer.getType());
        response.put("email", employer.getUser().getEmail());
        response.put("phone", employer.getUser().getPhone());
        response.put("verificationStatus", employer.getStatus());

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        List<JobPost> openJobsList = jobPostRepository.findJobsByEmployerAndStatus(
                employerId, "OPEN", limit, offset
        );
        long totalOpenJobs = jobPostRepository.countJobsByEmployerAndStatus(employerId, "OPEN");

        Applicant finalApplicant = currentApplicant;
        List<JobPostSummaryDTO> openJobDTOs = openJobsList.stream()
                .map(post -> mapToSummaryDTO(post, finalApplicant))
                .toList();

        Page<JobPostSummaryDTO> openJobsPage = new PageImpl<>(openJobDTOs, pageable, totalOpenJobs);
        response.put("openJobs", openJobsPage);


        List<JobPost> closedJobsList = jobPostRepository.findJobsByEmployerAndStatus(
                employerId, "CLOSED", limit, offset
        );
        long totalClosedJobs = jobPostRepository.countJobsByEmployerAndStatus(employerId, "CLOSED");

        List<JobPostSummaryDTO> closedJobDTOs = closedJobsList.stream()
                .map(post -> mapToSummaryDTO(post, finalApplicant))
                .toList();

        Page<JobPostSummaryDTO> closedJobsPage = new PageImpl<>(closedJobDTOs, pageable, totalClosedJobs);
        response.put("closedJobs", closedJobsPage);

        return ResponseEntity.ok(response);
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
                .orgName(jobPost.getEmployer() != null ? jobPost.getEmployer().getOrgName() : "Unknown")
                .jobType(String.valueOf(jobPost.getType()))
                .salary(formatSalary(jobPost.getSalaryMin(), jobPost.getSalaryMax()))
                .createdAt(jobPost.getCreatedAt())
                .isSaved(isSaved)
                .isApplied(isApplied)
                .build();
    }
    private String formatSalary(BigDecimal min, BigDecimal max) {
        return (min != null && max != null) ? min.toPlainString() + " - " + max.toPlainString() : "Thương lượng";
    }
    public ResponseEntity<?> getApplicantProfile(String token, String applicantId) {
        Map<String, Object> response = new HashMap<>();
        String email = jwtUtil.extractEmail(token);

        if (!tokenService.validateToken(token, email)) {
            response.put("status", "fail");
            response.put("message", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Optional<Applicant> applicantOptional = applicantRepository.findById(applicantId);
        if(applicantOptional.isEmpty()) {

            response.put("status", "fail");
            response.put("message", "Không tìm thấy thông tin người dùng.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Applicant applicant = applicantOptional.get();

        User user = applicant.getUser();
        Map<String, Object> userInfor = getUserInfor(user);
        response.putAll(userInfor);

        response.put("status", "success");
        response.put("applicantId", applicant.getId());
        response.put("userId", applicant.getUser().getId());
        response.put("resume", applicant.getResume());
        response.put("phone",applicant.getUser().getPhone());
        response.put("email",applicant.getUser().getEmail());
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> updateAvatar(String token, MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        String email = jwtUtil.extractEmail(token);
        if (!tokenService.validateToken(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Phiên đăng nhập đã hết hạn."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Người dùng không tồn tại."));
        }

        try {
            String avatarUrl = cloudinaryService.uploadFile(file);
            User user = userOpt.get();
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            response.put("status", "success");
            response.put("avatarUrl", avatarUrl);
            response.put("message", "Cập nhật ảnh đại diện thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "fail");
            response.put("message", "Lỗi trong quá trình tải ảnh lên: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<?> updateProfile(String token, UpdateProfile dto) {
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Truy cập trái phép."));
        }

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy người dùng."));
        }

        if (dto.getName() == null || dto.getPhoneNumber() == null || dto.getLocation() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp đầy đủ thông tin bắt buộc."));
        }

        user.setFullName(dto.getName());
        user.setPhone(dto.getPhoneNumber());
        user.setAddress(dto.getLocation());
        user.setGender(dto.getGender());
        user.setDateOfBirth(dto.getDateOfBirth());

        try {
            if ("Applicant".equals(user.getRole())) {
                Applicant applicant = applicantRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu ứng viên."));
                applicant.setResume(dto.getResume());
                applicantRepository.save(applicant);
            } else if ("Employer".equals(user.getRole())) {
                Employer employer = employerRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu nhà tuyển dụng."));

                try {
                    employer.setType(EmployerType.valueOf(dto.getEmployerType()));
                } catch (Exception e) {
                    employer.setType(EmployerType.Other);
                    employer.setCustomType(dto.getEmployerType());
                }
                employerRepository.save(employer);
            }
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Cập nhật hồ sơ thành công."));
        } catch (Exception e) {
            log.error("Update profile failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi khi cập nhật hồ sơ: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> updateEmployerProfileWithS3(String token, EmployerUpdateDTO dto) {
        String email = jwtUtil.extractEmail(token);
        if (!tokenService.validateToken(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Phiên làm việc không hợp lệ."));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy tài khoản người dùng."));
        }

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhà tuyển dụng."));

        try {
            if (dto.getBusinessLicense() != null && !dto.getBusinessLicense().isEmpty()) {
                String licenseUrl = s3Service.uploadFile(dto.getBusinessLicense());
                employer.setBusinessLicenseUrl(licenseUrl);
            }

            if (dto.getIdCardFront() != null && !dto.getIdCardFront().isEmpty()) {
                String idCardUrl = s3Service.uploadFile(dto.getIdCardFront());
                employer.setIdCardFront(idCardUrl);
            }

            if (dto.getFullName() != null) user.setFullName(dto.getFullName());
            if (dto.getPhoneNumber() != null) user.setPhone(dto.getPhoneNumber());
            if (dto.getAddress() != null) user.setAddress(dto.getAddress());
            if (dto.getGender() != null) user.setGender(dto.getGender());
            if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());

            if (dto.getOrgName() != null) employer.setOrgName(dto.getOrgName());
            if (dto.getTaxCode() != null) employer.setTaxCode(dto.getTaxCode());
            if (dto.getBusinessCode() != null) employer.setBusinessCode(dto.getBusinessCode());
            if (dto.getCompanyWebsite() != null) employer.setCompanyWebsite(dto.getCompanyWebsite());
            if (dto.getCompanyEmail() != null) employer.setCompanyEmail(dto.getCompanyEmail());
            if (dto.getHeadquartersAddress() != null) employer.setHeadquartersAddress(dto.getHeadquartersAddress());
            if (dto.getIdCardNumber() != null) employer.setIdCardNumber(dto.getIdCardNumber());

            if (dto.getEmployerType() != null) {
                try {
                    employer.setType(EmployerType.valueOf(dto.getEmployerType()));
                } catch (IllegalArgumentException e) {
                    employer.setType(EmployerType.Other);
                    employer.setCustomType(dto.getEmployerType());
                }
            }

            if (dto.getCustomType() != null) employer.setCustomType(dto.getCustomType());

            userRepository.save(user);
            employerRepository.save(employer);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Hồ sơ doanh nghiệp đã được cập nhật thành công.", "employer", employer));

        } catch (IOException e) {
            log.error("S3 Upload error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Gặp sự cố khi lưu trữ tài liệu lên hệ thống Cloud."));
        } catch (Exception e) {
            log.error("Update failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }


    public ResponseEntity<?> checkEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Duplicate email");
            response.put("message", "Địa chỉ email này đã được đăng ký trước đó.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(Map.of("status", "success", "message", "Email có thể sử dụng."));
    }

    public String deleteUser(long id) {
        if (!userRepository.existsById(id)) throw new RuntimeException("Người dùng không tồn tại.");
        userRepository.deleteById(id);
        return "success";
    }

    public ResponseEntity<?> checkPermission(String token, String role) {
        if (jwtUtil.checkPermission(token, role)) {
            return ResponseEntity.ok(Map.of("result", "accept"));
        }
        return new ResponseEntity<>(Map.of("result", "deny", "message", "Bạn không có quyền truy cập vào khu vực này."), HttpStatus.UNAUTHORIZED);
    }

    public ResponseEntity<?> isTokenValid(String token, String email) {
        if (tokenService.validateToken(token, email)) {
            return ResponseEntity.ok(Map.of("result", "valid"));
        }
        return new ResponseEntity<>(Map.of("result", "invalid"), HttpStatus.UNAUTHORIZED);
    }

    public ResponseEntity<?> checkPassword(String token, String password) {
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy người dùng."));
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu cung cấp không chính xác."));
        }
        return ResponseEntity.ok(Map.of("status", "success", "message", "Xác thực mật khẩu thành công."));
    }

    public ResponseEntity<?> sendResetCode(String email) {
        Map<String, Object> response = new HashMap<>();
        if (userRepository.findByEmail(email).isEmpty()) {
            response.put("error", "Not found");
            response.put("message", "Email không tồn tại trong hệ thống của chúng tôi.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String code = generateVerificationCode();
        long expiryTime = System.currentTimeMillis() + 5 * 60 * 1000;

        passwordResetMap.put(email, new VerificationInfo(code, expiryTime));

        try {
            mailService.sendResetPasswordCode(email, code);
        } catch (MessagingException e) {
            throw new RuntimeException("Gặp lỗi khi gửi email khôi phục mật khẩu.");
        }

        return ResponseEntity.ok(Map.of("status", "success", "message", "Mã khôi phục đã được gửi tới email của bạn."));
    }

    public ResponseEntity<?> resetPassword(String email, String code, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu xác nhận không trùng khớp."));
        }

        VerificationInfo info = passwordResetMap.get(email);
        if (info == null || info.isExpired() || !info.getCode().equalsIgnoreCase(code)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã khôi phục không hợp lệ hoặc đã hết hạn sử dụng."));
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetMap.remove(email);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Mật khẩu của bạn đã được thay đổi thành công."));
    }

    @Transactional
    public ResponseEntity<?> requestRegisterEmployer(String token, EmployerUpdateDTO dto) {
        String email = jwtUtil.extractEmail(token);
        if (!tokenService.validateToken(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Phiên làm việc không hợp lệ."));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy tài khoản người dùng."));
        }

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhà tuyển dụng."));
        if(employerRequestRepository.findByEmployer(employer).isPresent()){
            return ResponseEntity.badRequest().body(Map.of("message","bạn đã gửi yêu cầu trước đó rồi, vui lòng đợi duyệt"));
        }
        try {
            if (dto.getBusinessLicense() != null && !dto.getBusinessLicense().isEmpty()) {
                String licenseUrl = s3Service.uploadFile(dto.getBusinessLicense());
                employer.setBusinessLicenseUrl(licenseUrl);
            }

            if (dto.getIdCardFront() != null && !dto.getIdCardFront().isEmpty()) {
                String idCardUrl = s3Service.uploadFile(dto.getIdCardFront());
                employer.setIdCardFront(idCardUrl);
            }

            if (dto.getFullName() != null) user.setFullName(dto.getFullName());
            if (dto.getPhoneNumber() != null) user.setPhone(dto.getPhoneNumber());
            if (dto.getAddress() != null) user.setAddress(dto.getAddress());
            if (dto.getGender() != null) user.setGender(dto.getGender());
            if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());

            if (dto.getOrgName() != null) employer.setOrgName(dto.getOrgName());
            if (dto.getTaxCode() != null) employer.setTaxCode(dto.getTaxCode());
            if (dto.getBusinessCode() != null) employer.setBusinessCode(dto.getBusinessCode());
            if (dto.getCompanyWebsite() != null) employer.setCompanyWebsite(dto.getCompanyWebsite());
            if (dto.getCompanyEmail() != null) employer.setCompanyEmail(dto.getCompanyEmail());
            if (dto.getHeadquartersAddress() != null) employer.setHeadquartersAddress(dto.getHeadquartersAddress());
            if (dto.getIdCardNumber() != null) employer.setIdCardNumber(dto.getIdCardNumber());

            if (dto.getEmployerType() != null) {
                try {
                    employer.setType(EmployerType.valueOf(dto.getEmployerType()));
                } catch (IllegalArgumentException e) {
                    employer.setType(EmployerType.Other);
                    employer.setCustomType(dto.getEmployerType());
                }
            }

            if (dto.getCustomType() != null) employer.setCustomType(dto.getCustomType());

            userRepository.save(user);
            employerRepository.save(employer);

            EmployerRequest request = new EmployerRequest();
            request.setEmployer(employer);
            request.setStatus(VerificationStatus.PENDING);
            employerRequestRepository.save(request);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Gửi yêu cầu xác minh doanh nghiệp thành công. Vui lòng chờ phê duyệt."));

        } catch (IOException e) {
            log.error("S3 Upload error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi tải tài liệu: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Verification Request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi xử lý yêu cầu: " + e.getMessage()));
        }
    }
    public ResponseEntity<?> getAllEmployerRequests(String token) {
        if (!jwtUtil.checkPermission(token, "admin") && !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access."));
        }

        List<EmployerRequest> requests = employerRequestRepository.findAllByOrderByCreatedAtDesc();

        List<EmployerRequestInfoDTO> dtos = requests.stream().map(req -> {
            Employer emp = req.getEmployer();
            User u = emp.getUser();

            return EmployerRequestInfoDTO.builder()
                    .requestId(req.getId())
                    .employerId(emp.getId())
                    .orgName(emp.getOrgName())
                    .fullName(u.getFullName())
                    .customType(emp.getCustomType())
                    .headquartersAddress(emp.getHeadquartersAddress())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .companyWebsite(emp.getCompanyWebsite())
                    .businessCode(emp.getBusinessCode())
                    .companyEmail(emp.getCompanyEmail())
                    .gender(u.getGender())
                    .employerType(emp.getType().toString())
                    .status(req.getStatus())
                    .createdAt(req.getCreatedAt())
                    .taxCode(emp.getTaxCode())
                    .businessLicenseUrl(emp.getBusinessLicenseUrl())
                    .idCardNumber(emp.getIdCardNumber())
                    .idCardFrontUrl(emp.getIdCardFront())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
    public ResponseEntity<?> getEmployerProfileForHim(String token) {
        Map<String, Object> response = new HashMap<>();
        String email = jwtUtil.extractEmail(token);
        // 1. Validate Token
        if (!tokenService.validateToken(token, email)) {
            response.put("status", "fail");
            response.put("message", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Employer employer = jwtUtil.getEmployer(token);
        if(employer == null) return ResponseEntity.badRequest().body(response);
        if(!employer.getUser().getEmail().equals(email)) {
            response.put("status", "fail");
            response.put("message", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        User currentUser = userRepository.findByEmail(email).orElse(null);
        Applicant currentApplicant = null;
        if (currentUser != null && "Applicant".equals(currentUser.getRole())) { // Check role nếu cần
            currentApplicant = applicantRepository.findByUserId(currentUser.getId()).orElse(null);
        }

        User user = employer.getUser();
        Map<String, Object> userInfor = getUserInfor(user);
        response.putAll(userInfor);

        response.put("status", "success");
        response.put("employerId", employer.getId());
        response.put("userId", employer.getUser().getId());
        response.put("organization", employer.getOrgName());
        response.put("field", employer.getType());
        response.put("email", employer.getUser().getEmail());
        response.put("phone", employer.getUser().getPhone());
        response.put("companyWebsite", employer.getCompanyWebsite());
        response.put("businessCode", employer.getBusinessCode());
        response.put("companyEmail", employer.getCompanyEmail());
        response.put("businessLicenseUrl", employer.getBusinessLicenseUrl());
        response.put("taxCode", employer.getTaxCode());
        response.put("idCardFrontUrl", employer.getIdCardFront());
        response.put("idCardNumber", employer.getIdCardNumber());
        response.put("verificationStatus", employer.getStatus());

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        List<JobPost> openJobsList = jobPostRepository.findJobsByEmployerAndStatus(
                employer.getId(), "OPEN", limit, offset
        );
        long totalOpenJobs = jobPostRepository.countJobsByEmployerAndStatus(employer.getId(), "OPEN");

        Applicant finalApplicant = currentApplicant;
        List<JobPostSummaryDTO> openJobDTOs = openJobsList.stream()
                .map(post -> mapToSummaryDTO(post, finalApplicant))
                .toList();

        Page<JobPostSummaryDTO> openJobsPage = new PageImpl<>(openJobDTOs, pageable, totalOpenJobs);
        response.put("openJobs", openJobsPage);


        List<JobPost> closedJobsList = jobPostRepository.findJobsByEmployerAndStatus(
                employer.getId(), "CLOSED", limit, offset
        );
        long totalClosedJobs = jobPostRepository.countJobsByEmployerAndStatus(employer.getId(), "CLOSED");

        List<JobPostSummaryDTO> closedJobDTOs = closedJobsList.stream()
                .map(post -> mapToSummaryDTO(post, finalApplicant))
                .toList();

        Page<JobPostSummaryDTO> closedJobsPage = new PageImpl<>(closedJobDTOs, pageable, totalClosedJobs);
        response.put("closedJobs", closedJobsPage);

        return ResponseEntity.ok(response);
    }
    private static class VerificationInfo {
        @lombok.Getter
        private final String code;
        private final long expiryTimeMillis;

        public VerificationInfo(String code, long expiryTimeMillis) {
            this.code = code;
            this.expiryTimeMillis = expiryTimeMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTimeMillis;
        }
    }

}