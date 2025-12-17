package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Auth.*;
import com.example.JFS_Job_Finding_Service.DTO.EmployerUpdateDTO;
import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Employer;
import com.example.JFS_Job_Finding_Service.models.Enum.EmployerType;
import com.example.JFS_Job_Finding_Service.models.Enum.VerificationStatus;
import com.example.JFS_Job_Finding_Service.models.PendingRegistration;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import com.example.JFS_Job_Finding_Service.repository.EmployerRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import com.example.JFS_Job_Finding_Service.security.PasswordConfig;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
            response.put("message", "Tên tổ chức không được để trống.");
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
    public ResponseEntity<?> acceptEmployerRegistration(String token, reviewEmployerDTO dto) {
        Map<String, Object> response = new HashMap<>();
        if(!jwtUtil.checkPermission(token,"admin")&&!jwtUtil.validateToken(token)){
            response.put("error", "Invalid access");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Employer employer =  employerRepository.findById(dto.getEmployerId()).orElse(null);
        if(employer == null) return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        employer.setStatus(VerificationStatus.VERIFIED);
        employerRepository.save(employer);
        return ResponseEntity.ok().build();
    }
    public ResponseEntity<?> rejectEmployerRegistration(String token, reviewEmployerDTO dto) {
        Map<String, Object> response = new HashMap<>();
        if(!jwtUtil.checkPermission(token,"admin")&&!jwtUtil.validateToken(token)){
            response.put("error", "Invalid access");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Employer employer =  employerRepository.findById(dto.getEmployerId()).orElse(null);
        if(employer == null) return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        employer.setStatus(VerificationStatus.REJECTED);
        employer.setRejectionReason(dto.getReason());
        employerRepository.save(employer);
        return ResponseEntity.ok().build();
    }
    public ResponseEntity<?> ApplicantRegister(ApplicantRegisterRequest request) {
        // Validate chung
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
            response.put("message", "Không thể gửi email xác nhận. Vui lòng thử lại sau.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        pendingRegisterMap.put(email, pendingRegistration);
        response.put("status", "success");
        response.put("message", "Mã xác nhận đã được gửi. Vui lòng kiểm tra email!");
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
            response.put("message", "Mật khẩu không khớp!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByEmail(email).isPresent()) {
            response.put("error", "Duplicate email");
            response.put("message", "Email này đã được sử dụng!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (name == null || !name.matches("^[\\p{L} ]+$")) {
            response.put("error", "Invalid name format");
            response.put("message", "Tên chỉ được chứa chữ cái và khoảng trắng.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (dob != null) {
            LocalDate birthDate = dob;
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            if (age < 15) {
                response.put("error", "Age violation");
                response.put("message", "Bạn phải từ 15 tuổi trở lên để đăng ký.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } else {
            response.put("error", "Missing Date");
            response.put("message", "Vui lòng nhập ngày sinh.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String normalizedGender = (gender != null) ? gender.toLowerCase() : "";
        if (!List.of("male", "female", "other").contains(normalizedGender)) {
            response.put("error", "Invalid gender");
            response.put("message", "Giới tính không hợp lệ (male, female, other).");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        return null; // No error
    }

    // --- Verification Logic ---

    public ResponseEntity<?> sendVerificationEmailHTML(String email) {
        Map<String, Object> response = new HashMap<>();
        PendingRegistration pendingReg = pendingRegisterMap.get(email);

        if (pendingReg == null) {
            response.put("error", "Invalid");
            response.put("message", "Yêu cầu đăng ký không tồn tại hoặc đã hết hạn!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String code = generateVerificationCode();
        pendingReg.setVerificationCode(code);
        pendingReg.setExpiryTime(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));

        pendingRegisterMap.put(email, pendingReg);

        try {
            mailService.sendVerificationEmailHTML(email, code);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        response.put("status", "success");
        response.put("message", "Mã xác nhận mới đã được gửi.");
        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> verifyEmail(VerifyEmailDTO verifyEmailDTO) {
        Map<String, Object> response = new HashMap<>();
        String email = verifyEmailDTO.getEmail();
        PendingRegistration pendingReg = pendingRegisterMap.get(email);

        if (pendingReg == null) {
            response.put("error", "Invalid request");
            response.put("message", "Yêu cầu không hợp lệ.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.now().isAfter(pendingReg.getExpiryTime())) {
            pendingRegisterMap.remove(email); // Xóa nếu đã hết hạn
            response.put("error", "Expired");
            response.put("message", "Mã xác nhận đã hết hạn!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!pendingReg.getVerificationCode().equalsIgnoreCase(verifyEmailDTO.getCode())) {
            response.put("error", "Wrong code");
            response.put("message", "Mã xác nhận không đúng.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User user = pendingReg.getUser();
        user.setActive(true);
        User savedUser = userRepository.save(user);

        if (pendingReg.getApplicant() != null) {
            Applicant applicant = pendingReg.getApplicant();
            applicant.setUser(savedUser); // Link lại ID đã save
            applicantRepository.save(applicant);
        } else if (pendingReg.getEmployer() != null) {
            Employer employer = pendingReg.getEmployer();
            employer.setUser(savedUser); // Link lại ID đã save
            employerRepository.save(employer);
        }

        pendingRegisterMap.remove(email); // Clean up
        response.put("status", "success");
        response.put("message", "Xác minh email thành công!");
        return ResponseEntity.ok(response);
    }

    // --- Login Logic ---

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
            response.put("message", "Tài khoản không tồn tại!");
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
            response.put("message", "Tài khoản không tồn tại hoặc sai vai trò!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            response.put("error", "Invalid password");
            response.put("message", "Sai mật khẩu!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!user.isActive()) {
            response.put("error", "User banned/inactive");
            response.put("message", "Tài khoản chưa kích hoạt hoặc bị khóa!");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        response.put("status", "success");
        response.put("token", token);
        response.put("user", user);

        if ("Employer".equals(requiredRole)) {
            employerRepository.findByUser(user).ifPresent(e -> response.put("employer", e));
        } else {
            applicantRepository.findByUser(user).ifPresent(a -> response.put("applicant", a));
        }

        return ResponseEntity.ok(response);
    }

    // --- Profile Management ---

    public ResponseEntity<?> getProfile(String token, Long userId) {
        Map<String, Object> response = new HashMap<>();
        String email = jwtUtil.extractEmail(token);

        if (!tokenService.validateToken(token, email)) {
            response.put("status", "fail");
            response.put("message", "Unauthorized access");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            response.put("status", "fail");
            response.put("message", "User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();

        // Build response common fields
        response.put("phone", user.getPhone());
        response.put("address", user.getAddress());
        response.put("gender", user.getGender());
        response.put("date_of_birth", user.getDateOfBirth());
        response.put("avatar", user.getAvatarUrl());
        response.put("createdAt", user.getCreatedAt());
        response.put("email", user.getEmail());
        response.put("name", user.getFullName());
        response.put("role", user.getRole());

        if ("Applicant".equals(user.getRole())) {
            Optional<Applicant> applicant = applicantRepository.findByUser(user);
            if (applicant.isPresent()) {
                response.put("status", "success");
                response.put("applicantId", applicant.get().getId());
                response.put("resume", applicant.get().getResume());
            } else {
                response.put("status", "fail");
                response.put("message", "Applicant profile missing");
            }
        } else if ("Employer".equals(user.getRole())) {
            Optional<Employer> employer = employerRepository.findByUser(user);
            if (employer.isPresent()) {
                response.put("status", "success");
                response.put("employerId", employer.get().getId());
                response.put("organization", employer.get().getOrgName());
                response.put("field", employer.get().getType());
                // Return verification info if needed
                response.put("verificationStatus", employer.get().getStatus());
            } else {
                response.put("status", "fail");
                response.put("message", "Employer profile missing");
            }
        }
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> updateAvatar(String token, MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        String email = jwtUtil.extractEmail(token);
        if (!tokenService.validateToken(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        try {
            String avatarUrl = cloudinaryService.uploadFile(file);
            User user = userOpt.get();
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            response.put("status", "success");
            response.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "fail");
            response.put("message", "Failed to upload: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<?> updateProfile(String token, UpdateProfile dto) {
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        // Basic Validation
        if (dto.getName() == null || dto.getPhoneNumber() == null || dto.getLocation() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Required fields missing"));
        }

        // Update User Common Fields
        user.setFullName(dto.getName());
        user.setPhone(dto.getPhoneNumber());
        user.setAddress(dto.getLocation());
        user.setGender(dto.getGender());
        user.setDateOfBirth(dto.getDateOfBirth());

        try {
            if ("Applicant".equals(user.getRole())) {
                Applicant applicant = applicantRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("Applicant data not found"));
                applicant.setResume(dto.getResume());
                applicantRepository.save(applicant);
            } else if ("Employer".equals(user.getRole())) {
                Employer employer = employerRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("Employer data not found"));

                // Update Employer Type safe
                try {
                    employer.setType(EmployerType.valueOf(dto.getEmployerType()));
                } catch (Exception e) {
                    employer.setType(EmployerType.Other);
                    employer.setCustomType(dto.getEmployerType());
                }

                // Nếu update org name cần thêm field trong DTO, hiện tại dùng getOrgName nếu DTO chưa có
                // employer.setOrgName(dto.getOrgName());

                employerRepository.save(employer);
            }
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Profile updated"));
        } catch (Exception e) {
            log.error("Update profile failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }


    public ResponseEntity<?> updateEmployerProfileWithS3(String token, EmployerUpdateDTO dto) {
        Map<String, Object> response = new HashMap<>();

        // 1. Validate Token
        String email = jwtUtil.extractEmail(token);
        if (!tokenService.validateToken(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        // 2. Find User and Employer
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        Employer employer = employerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Employer profile not found"));

        try {
            // 3. Upload Files to AWS S3 (if provided)
            if (dto.getBusinessLicense() != null && !dto.getBusinessLicense().isEmpty()) {
                String licenseUrl = s3Service.uploadFile(dto.getBusinessLicense());
                employer.setBusinessLicenseUrl(licenseUrl);
            }

            if (dto.getIdCardFront() != null && !dto.getIdCardFront().isEmpty()) {
                String idCardUrl = s3Service.uploadFile(dto.getIdCardFront());
                employer.setIdCardFront(idCardUrl);
            }

            // 4. Update User Basic Info
            if (dto.getFullName() != null) user.setFullName(dto.getFullName());
            if (dto.getPhoneNumber() != null) user.setPhone(dto.getPhoneNumber());
            if (dto.getAddress() != null) user.setAddress(dto.getAddress());
            if (dto.getGender() != null) user.setGender(dto.getGender());
            if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());

            // 5. Update Employer Specific Info
            if (dto.getOrgName() != null) employer.setOrgName(dto.getOrgName());
            if (dto.getTaxCode() != null) employer.setTaxCode(dto.getTaxCode());
            if (dto.getBusinessCode() != null) employer.setBusinessCode(dto.getBusinessCode());
            if (dto.getCompanyWebsite() != null) employer.setCompanyWebsite(dto.getCompanyWebsite());
            if (dto.getCompanyEmail() != null) employer.setCompanyEmail(dto.getCompanyEmail());
            if (dto.getHeadquartersAddress() != null) employer.setHeadquartersAddress(dto.getHeadquartersAddress());
            if (dto.getIdCardNumber() != null) employer.setIdCardNumber(dto.getIdCardNumber());

            // Handle Enum safely
            if (dto.getEmployerType() != null) {
                try {
                    employer.setType(EmployerType.valueOf(dto.getEmployerType()));
                } catch (IllegalArgumentException e) {
                    employer.setType(EmployerType.Other);
                    employer.setCustomType(dto.getEmployerType());
                }
            }

            if (dto.getCustomType() != null) employer.setCustomType(dto.getCustomType());

            // 6. Save changes
            userRepository.save(user);
            employerRepository.save(employer);

            response.put("status", "success");
            response.put("message", "Employer profile updated successfully");
            response.put("employer", employer);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("S3 Upload error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload files to storage"));
        } catch (Exception e) {
            log.error("Update failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    public ResponseEntity<?> UpdateResume(String token, String email, Map<String, Object> resume) {
        if (!tokenService.validateToken(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));

        Applicant applicant = applicantRepository.findByUser(user).orElse(null);
        if (applicant == null) return ResponseEntity.badRequest().body(Map.of("message", "Applicant not found"));

        applicant.setResume(resume);
        applicantRepository.save(applicant);

        return ResponseEntity.ok(Map.of("status", "success", "user", user));
    }

    // --- Utility Methods ---

    public ResponseEntity<?> checkEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Duplicate email");
            response.put("message", "Email đã được sử dụng!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    public String updatePassword(String email, String oldPass, String password, String confirmPass) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User does not exist"));
        if (!passwordEncoder.matches(oldPass, user.getPassword())) throw new RuntimeException("Invalid password");
        if (!password.equals(confirmPass)) throw new RuntimeException("Passwords do not match");

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return "success";
    }

    public String deleteUser(long id) {
        if (!userRepository.existsById(id)) throw new RuntimeException("User does not exist");
        userRepository.deleteById(id);
        return "success";
    }

    public ResponseEntity<?> checkPermission(String token, String role) {
        if (jwtUtil.checkPermission(token, role)) {
            return ResponseEntity.ok(Map.of("result", "accept"));
        }
        return new ResponseEntity<>(Map.of("result", "deny"), HttpStatus.UNAUTHORIZED);
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
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Wrong password"));
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // --- Reset Password Logic ---

    public ResponseEntity<?> sendResetCode(String email) {
        Map<String, Object> response = new HashMap<>();
        if (userRepository.findByEmail(email).isEmpty()) {
            response.put("error", "Not found");
            response.put("message", "Email không tồn tại!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String code = generateVerificationCode();
        long expiryTime = System.currentTimeMillis() + 5 * 60 * 1000;

        passwordResetMap.put(email, new VerificationInfo(code, expiryTime));

        try {
            mailService.sendResetPasswordCode(email, code);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }

        return ResponseEntity.ok(Map.of("status", "success", "message", "Mã xác nhận đã gửi."));
    }

    public ResponseEntity<?> resetPassword(String email, String code, String newPassword, String confirmPassword) {
        Map<String, Object> response = new HashMap<>();
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu không khớp!"));
        }

        VerificationInfo info = passwordResetMap.get(email);
        if (info == null || info.isExpired() || !info.getCode().equalsIgnoreCase(code)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã không hợp lệ hoặc đã hết hạn!"));
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetMap.remove(email);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Đặt lại mật khẩu thành công!"));
    }

    // Helper class cho Reset Password (giữ nguyên logic cũ nhưng clean hơn)
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