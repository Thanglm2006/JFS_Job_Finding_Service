package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.Admin;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.AdminRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {
    @Autowired
    AdminRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Value("${secretPass}")
    private String secretPass;
    public ResponseEntity<Admin> addAdmin(String secretPass,String fullName, String email, String password) {
        if (!this.secretPass.equals(secretPass)) {
            return ResponseEntity.status(403).body(null);
        }
        if (repository.findByEmail(email) != null) {
            return ResponseEntity.status(400).body(null);
        }
        String encodedPass= passwordEncoder.encode(password);
        Admin admin = new Admin();
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPassword(encodedPass);
        repository.save(admin);
        return ResponseEntity.ok(admin);

    }
    public ResponseEntity<?> login(String email, String password) {
        Admin admin = repository.findByEmail(email);
        if (admin == null) {
            Map<String, String> error = Map.of("message", "Admin not found");
            return ResponseEntity.status(404).body(error);
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            Map<String, String> error = Map.of("message", "Invalid password");
            return ResponseEntity.status(401).body(error);
        }

        String token = jwtUtil.generateToken(email, "Admin");
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", email);
        response.put("role", "Admin");
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> getAllUsers(String token, int page, int size) {
        boolean check = jwtUtil.checkPermission(token, "Admin");
        Map<String, Object> response = new HashMap<>();
        if (!check) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return ResponseEntity.status(403).body(response);
        }
        try{
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findAll(pageable);
            response.put("status", "success");
            response.put("users", users.getContent());
            response.put("totalPages", users.getTotalPages());
            response.put("totalElements", users.getTotalElements());
            return ResponseEntity.ok(response);

        }
        catch (Exception e) {
            response.put("status", "fail");
            response.put("message", "Lỗi khi lấy danh sách người dùng: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    public ResponseEntity<?> banUser(String token, long userId) {
        boolean check = jwtUtil.checkPermission(token, "Admin");
        Map<String, Object> response = new HashMap<>();
        if (!check) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return ResponseEntity.status(403).body(response);
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không tồn tại");
            return ResponseEntity.status(404).body(response);
        }
        if( !user.isActive()) {
            response.put("status", "fail");
            response.put("message", "Người dùng đã bị cấm trước đó");
            return ResponseEntity.status(400).body(response);
        }
        user.setActive(false);
        userRepository.save(user);
        response.put("status", "success");
        response.put("message", "Người dùng đã bị cấm thành công");
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> unbanUser(String token, long userId) {
        boolean check = jwtUtil.checkPermission(token, "Admin");
        Map<String, Object> response = new HashMap<>();
        if (!check) {
            response.put("status", "fail");
            response.put("message", "bạn không có quyền truy cập");
            return ResponseEntity.status(403).body(response);
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không tồn tại");
            return ResponseEntity.status(404).body(response);
        }
        if(user.isActive()) {
            response.put("status", "fail");
            response.put("message", "Người dùng không bị cấm");
            return ResponseEntity.status(400).body(response);
        }
        user.setActive(true);
        userRepository.save(user);
        response.put("status", "success");
        response.put("message", "Người dùng đã được mở khóa thành công");
        return ResponseEntity.ok(response);
    }
}
