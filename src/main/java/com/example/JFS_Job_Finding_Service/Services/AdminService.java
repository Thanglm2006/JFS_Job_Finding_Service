package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Schedule.UserOutAdmin;
import com.example.JFS_Job_Finding_Service.models.Admin;
import com.example.JFS_Job_Finding_Service.models.Notification; // Import
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.*;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant; // Import
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    @Autowired
    AdminRepository adminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository; // Đã thê
    @Autowired
    private JwtUtil jwtUtil;
    @Value("${secretPass}")
    private String secretPass;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RestClient.Builder builder;
    private EmployerRequestRepository employerRequestRepository;
    @Autowired
    private EmployerRepository employerRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicantRepository applicantRepository;

    @Transactional

    public ResponseEntity<Admin> addAdmin(String secretPass,String fullName, String email, String password) {
        if (!this.secretPass.equals(secretPass)) {
            return ResponseEntity.status(403).body(null);
        }
        if (adminRepository.findByEmail(email) != null) {
            return ResponseEntity.status(400).body(null);
        }
        String encodedPass= passwordEncoder.encode(password);
        Admin admin = new Admin();
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPassword(encodedPass);
        adminRepository.save(admin);
        return ResponseEntity.ok(admin);

    }
    @Transactional
    public ResponseEntity<?> login(String email, String password) {
        Admin admin = adminRepository.findByEmail(email);
        if (admin == null) {
            Map<String, String> error = Map.of("message", "Không tìm thấy quản trị viên với email này.");
            return ResponseEntity.status(404).body(error);
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            Map<String, String> error = Map.of("message", "Mật khẩu không chính xác. Vui lòng thử lại.");
            return ResponseEntity.status(401).body(error);
        }

        String token = jwtUtil.generateToken(email, "Admin");
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", email);
        response.put("role", "Admin");
        return ResponseEntity.ok(response);
    }
    @Transactional
    public ResponseEntity<?> getAllUsers(String token, int page, int size) {
        boolean check = jwtUtil.checkPermission(token, "Admin")&& tokenService.validateToken(token);
        Map<String, Object> response = new HashMap<>();
        if (!check) {
            response.put("status", "fail");
            response.put("message", "Rất tiếc, bạn không có quyền truy cập vào danh sách này.");
            return ResponseEntity.status(403).body(response);
        }
        try{
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findAll(pageable);
            List<UserOutAdmin> userList = new ArrayList<>();
            for(User user : users.getContent()){
                UserOutAdmin userOutAdmin =  new UserOutAdmin();
                userOutAdmin.setRole(user.getRole());
                userOutAdmin.setFullName(user.getFullName());
                userOutAdmin.setUserId(user.getId());
                if(employerRepository.findByUser(user).isPresent()){
                    userOutAdmin.setEmployerId(employerRepository.findByUser(user).get().getId());
                }
                else if(applicantRepository.findByUser(user).isPresent()){
                    userOutAdmin.setApplicantId(applicantRepository.findByUser(user).get().getId());
                }
                userOutAdmin.setAvatarUrl(user.getAvatarUrl());
                userOutAdmin.setActive(user.isActive());
                userList.add(userOutAdmin);
            }
            response.put("status", "success");
            response.put("users", userList);
            response.put("totalPages", users.getTotalPages());
            response.put("totalElements", users.getTotalElements());
            return ResponseEntity.ok(response);

        }
        catch (Exception e) {
            response.put("status", "fail");
            response.put("message", "Đã xảy ra lỗi khi lấy danh sách người dùng: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    @Transactional

    public ResponseEntity<?> banUser(String token, long userId) {
        boolean check = jwtUtil.checkPermission(token, "Admin")&& tokenService.validateToken(token);
        Map<String, Object> response = new HashMap<>();
        if (!check) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền thực hiện thao tác cấm người dùng.");
            return ResponseEntity.status(403).body(response);
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không tồn tại trên hệ thống.");
            return ResponseEntity.status(404).body(response);
        }
        if( !user.isActive()) {
            response.put("status", "fail");
            response.put("message", "Tài khoản người dùng này đã bị cấm từ trước.");
            return ResponseEntity.status(400).body(response);
        }
        user.setActive(false);
        userRepository.save(user);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage("Tài khoản của bạn đã bị khóa bởi quản trị viên.");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        response.put("status", "success");
        response.put("message", "Đã cấm người dùng thành công.");
        return ResponseEntity.ok(response);
    }
    @Transactional

    public ResponseEntity<?> unbanUser(String token, long userId) {
        boolean check = jwtUtil.checkPermission(token, "Admin")&& tokenService.validateToken(token);
        Map<String, Object> response = new HashMap<>();
        if (!check) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền thực hiện thao tác mở khóa người dùng.");
            return ResponseEntity.status(403).body(response);
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không tồn tại trên hệ thống.");
            return ResponseEntity.status(404).body(response);
        }
        if(user.isActive()) {
            response.put("status", "fail");
            response.put("message", "Tài khoản người dùng hiện không bị cấm.");
            return ResponseEntity.status(400).body(response);
        }
        user.setActive(true);
        userRepository.save(user);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage("Tài khoản của bạn đã được mở khóa bởi quản trị viên.");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        response.put("status", "success");
        response.put("message", "Đã mở khóa người dùng thành công.");
        return ResponseEntity.ok(response);
    }
    @Transactional

    public ResponseEntity<?> deleteUser(String token, long userId) {
        boolean check = jwtUtil.checkPermission(token, "Admin")&& tokenService.validateToken(token);
        Map<String, Object> response = new HashMap<>();
        if (!check) {
            response.put("status", "fail");
            response.put("message", "Bạn không có quyền xóa người dùng.");
            return ResponseEntity.status(403).body(response);
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("status", "fail");
            response.put("message", "Người dùng không tồn tại trên hệ thống.");
            return ResponseEntity.status(404).body(response);
        }
        userRepository.delete(user);
        response.put("status", "success");
        response.put("message", "Đã xóa người dùng khỏi hệ thống thành công.");
        return ResponseEntity.ok(response);
    }
}