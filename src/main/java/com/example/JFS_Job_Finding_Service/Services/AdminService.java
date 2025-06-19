package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.models.Admin;
import com.example.JFS_Job_Finding_Service.repository.AdminRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
}
