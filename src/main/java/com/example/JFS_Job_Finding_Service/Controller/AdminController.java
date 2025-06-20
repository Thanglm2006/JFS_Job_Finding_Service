package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.Services.AdminService;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
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
}
