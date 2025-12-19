package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.DTO.Auth.reviewEmployerDTO;
import com.example.JFS_Job_Finding_Service.DTO.Employer.ReviewEmployerDTO;
import com.example.JFS_Job_Finding_Service.Services.AdminService;
import com.example.JFS_Job_Finding_Service.Services.UserService;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @GetMapping("/getAllUsers")
    public ResponseEntity<?> getAllUsers(@RequestHeader HttpHeaders headers, @RequestParam("page") int page) {
       return adminService.getAllUsers(headers.getFirst("token"), page, 10);
    }
    @PostMapping("/banUser")
    public ResponseEntity<?> banUser(@RequestHeader HttpHeaders headers, @RequestParam("userId") Long userId) {
      return adminService.banUser(headers.getFirst("token"), userId);
    }
    @PostMapping("/unbanUser")
    public ResponseEntity<?> unbanUser(@RequestHeader HttpHeaders headers, @RequestParam("userId") Long userId) {
        return adminService.unbanUser(headers.getFirst("token"), userId);
    }
    @PostMapping("/deleteUser")
    public ResponseEntity<?> deleteUser(@RequestHeader HttpHeaders headers, @RequestParam("userId") Long userId) {
        return adminService.deleteUser(headers.getFirst("token"), userId);
    }
    // ================= ADMIN: EMPLOYER REQUEST MANAGEMENT =================

    @GetMapping("/employer-requests")
    @Operation(summary = "Get all employer verification requests (Admin only)")
    public ResponseEntity<?> getAllEmployerRequests(@RequestHeader("token") String token) {
        return userService.getAllEmployerRequests(token);
    }

    @PostMapping("/approve-employer")
    @Operation(summary = "Approve an employer verification request (Admin only)")
    public ResponseEntity<?> approveEmployerRequest(
            @RequestHeader("token") String token,
            @RequestBody ReviewEmployerDTO dto
    ) {
        return userService.acceptEmployerRegistration(token, dto);
    }

    @PostMapping("/reject-employer")
    @Operation(summary = "Reject an employer verification request (Admin only)")
    public ResponseEntity<?> rejectEmployerRequest(
            @RequestHeader("token") String token,
            @RequestBody ReviewEmployerDTO dto
    ) {
        return userService.rejectEmployerRegistration(token, dto);
    }
}
