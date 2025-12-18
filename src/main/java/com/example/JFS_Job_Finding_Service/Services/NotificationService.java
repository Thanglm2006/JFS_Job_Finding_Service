package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.Admin;
import com.example.JFS_Job_Finding_Service.models.Notification;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.AdminRepository;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private UserRepository userRepository;

    public ResponseEntity<?> setIsRead(Long notificationId){
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo với ID: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Đã đánh dấu thông báo là đã đọc.");
    }
    public ResponseEntity<?> getAllNotifications(String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Truy cập trái phép. Vui lòng đăng nhập lại.");
        }
        boolean checkAdmin = jwtUtil.checkPermission(token,"Admin");
        String email = jwtUtil.extractEmail(token);
        if(checkAdmin){

            Admin admin = adminRepository.findByEmail(email);
            if (admin == null) {
                return ResponseEntity.status(403).body("Bạn không có quyền truy cập vào thông báo hệ thống.");
            }
            return new ResponseEntity<>(notificationRepository.findAll(), HttpStatus.OK);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        return new ResponseEntity<>(notificationRepository.findByUser(user), HttpStatus.OK);
    }
}