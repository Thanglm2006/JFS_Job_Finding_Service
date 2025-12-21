package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Auth.*;
import com.example.JFS_Job_Finding_Service.DTO.Employer.EmployerUpdateDTO;
import com.example.JFS_Job_Finding_Service.DTO.Employer.ReviewEmployerDTO;
import com.example.JFS_Job_Finding_Service.Services.AdminService;
import com.example.JFS_Job_Finding_Service.Services.ApplicationService;
import com.example.JFS_Job_Finding_Service.Services.RedisTokenService;
import com.example.JFS_Job_Finding_Service.Services.UserService;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private RedisTokenService redisTokenService;

    // ================= REGISTRATION =================

    @PostMapping("/register/employer")
    @Operation(summary = "User Registration", description = "Register a new user with email, password, name, and role.")
    public ResponseEntity<?> EmployerRegister(@RequestBody EmployerRegisterRequest employerRegisterRequest) {
        return userService.EmployerRegister(employerRegisterRequest);
    }

    @PostMapping("/register/applicant")
    @Operation(summary = "User Registration", description = "Register a new user with email, password, name, and role.")
    public ResponseEntity<?> ApplicantRegister(@RequestBody ApplicantRegisterRequest applicantRegisterRequest) {
        return userService.ApplicantRegister(applicantRegisterRequest);
    }

    @PostMapping("/register/admin")
    @Operation(summary = "Admin Registration", description = "Register a new admin with email, password, name, and secret pass.")
    public ResponseEntity<?> AdminRegister(@RequestBody AdminRegisterRequest request) {
        // Using DTO instead of Map
        return adminService.addAdmin(request.getSecretPass(), request.getFullName(), request.getEmail(), request.getPassword());
    }

    // ================= LOGIN =================

    @PostMapping("/login/employer")
    @Operation(summary = "User Login", description = "Authenticate a user and return a JWT token.")
    public ResponseEntity<?> EmployerLogin(@RequestBody LoginRequest loginRequest) {
        try {
            return userService.EmployerLogin(loginRequest.getEmail(), loginRequest.getPassword());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/login/applicant")
    @Operation(summary = "User Login", description = "Authenticate a user and return a JWT token.")
    public ResponseEntity<?> ApplicantLogin(@RequestBody LoginRequest loginRequest) {
        try {
            return userService.ApplicantLogin(loginRequest.getEmail(), loginRequest.getPassword());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/login/admin")
    @Operation(summary = "Admin Login", description = "Authenticate an admin and return a JWT token.")
    public ResponseEntity<?> AdminLogin(@RequestBody LoginRequest loginRequest) {
        try {
            return adminService.login(loginRequest.getEmail(), loginRequest.getPassword());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // ================= VERIFICATION & PASSWORD =================

    @PostMapping("/verifyEmail")
    @Operation(summary = "verify email", description = "send a verification code to the email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailDTO verifyEmailDTO) {
        return userService.verifyEmail(verifyEmailDTO);
    }

    @PostMapping("/sendVerificationCode")
    @Operation(summary = "send verification code", description = "send a verification code to the email")
    public ResponseEntity<?> sendVerificationCode(@RequestBody SendVerificationCodeRequest request) {
        return userService.sendVerificationEmailHTML(request.getEmail());
    }

    @PostMapping("/forgetPassword")
    @Operation(summary = "forget password", description = "send a verification code to the email for resetting password")
    public ResponseEntity<?> forgetPassword(@RequestBody ForgetPasswordRequest request) {
        return userService.sendResetCode(request.getEmail());
    }

    @PostMapping("/changePassword")
    @Operation(summary = "change password", description = "change the password using the verification code")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        return userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword(), request.getConfirmPassword());
    }

    // ================= CHECKS & VALIDATION =================

    @PostMapping("/checkEmail")
    @Operation(summary = "check whether the email is already used")
    public ResponseEntity<?> checkEmail(@RequestBody CheckEmailRequest request) {
        return userService.checkEmail(request.getEmail());
    }

    @PostMapping("/checkPass")
    @Operation(summary = "check whether the password is correct", description = "use token and password for checking process!")
    public ResponseEntity<?> checkPass(@RequestHeader HttpHeaders headers, @RequestBody CheckPassRequest checkPassRequest) {
        if (headers.get("token") == null || headers.get("token").isEmpty()) {
            return ResponseEntity.status(401).body("Token is missing");
        }
        return userService.checkPassword(headers.get("token").get(0), checkPassRequest.getPassword());
    }

    @PostMapping("/checkPermission")
    @Operation(summary = "check whether the permission is right")
    public ResponseEntity<?> checkPermission(@RequestBody CheckPermissionRequest request) {
        return userService.checkPermission(request.getToken(), request.getRole());
    }

    @PostMapping("/isTokenValid")
    @Operation(summary = "check whether the token is valid")
    public ResponseEntity<?> isTokenValid(@RequestHeader HttpHeaders headers) {
        if (headers != null && headers.get("authorization") != null && headers.get("email") != null) {
            return userService.isTokenValid(headers.get("authorization").get(0).substring(7), headers.get("email").get(0));
        } else {
            return ResponseEntity.status(401).body("Token or Email header is null");
        }
    }

    // ================= PROFILE MANAGEMENT =================

    @PostMapping("/ApplicantProfile")
    @Operation(summary = "get profile information")
    public ResponseEntity<?> getApplicantProfile(@RequestHeader HttpHeaders headers, @RequestParam("id") String applicantId) {
        if (headers != null && headers.get("token") != null) {
            return userService.getApplicantProfile(headers.getFirst("token"), applicantId);
        } else {
            return ResponseEntity.status(401).body("Token is null");
        }
    }
    @PostMapping("/EmployerProfile")
    @Operation(summary = "get profile information")
    public ResponseEntity<?> getEmployerProfile(@RequestHeader HttpHeaders headers, @RequestParam("id") String employerId) {
        if (headers != null && headers.get("token") != null) {
            return userService.getEmployerProfile(headers.getFirst("token"), employerId);
        } else {
            return ResponseEntity.status(401).body("Token is null");
        }
    }
    @PostMapping("/updateProfile")
    @Operation(summary = "update basic profile information")
    public ResponseEntity<?> updateProfile(@RequestHeader HttpHeaders headers, @RequestBody UpdateProfile body) {
        if (headers != null && headers.get("token") != null) {
            return userService.updateProfile(headers.getFirst("token"), body);
        } else {
            return ResponseEntity.status(401).body("Token is null");
        }
    }

    @PostMapping("/updateAvatar")
    @Operation(summary = "update avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        if (headers != null && headers.get("token") != null) {
            return userService.updateAvatar(headers.getFirst("token"), avatar);
        } else {
            return ResponseEntity.status(401).body("Token is null");
        }
    }

    @GetMapping("/logout")
    @Operation(summary = "move token to blacklist")
    public ResponseEntity<?> moveTokenToBlacklist(@RequestHeader HttpHeaders headers) {
        if (headers != null && headers.get("token") != null) {
            return redisTokenService.blacklistToken(headers.getFirst("token"));
        } else {
            return ResponseEntity.ok().build();
        }
    }

    // ================= EMPLOYER SPECIFIC =================

    @PostMapping(value = "/update-employer-profile")
    @Operation(summary = "Update employer profile with files (S3)")
    public ResponseEntity<?> updateEmployerProfile(
            @ModelAttribute EmployerUpdateDTO dto,
            @RequestHeader("token") String token
    ) {
        return userService.updateEmployerProfileWithS3(token, dto);
    }

    @PostMapping(value = "/employer/request-verification")
    @Operation(summary = "Submit employer documents for verification (Creates a Request)")
    public ResponseEntity<?> requestEmployerVerification(
            @ModelAttribute EmployerUpdateDTO dto,
            @RequestHeader("token") String token
    ) {
        // This endpoint calls the logic that creates an EmployerRequest entry
        return userService.requestRegisterEmployer(token, dto);
    }


}