package com.example.JFS_Job_Finding_Service.Services;
import com.example.JFS_Job_Finding_Service.DTO.UpdateProfile;
import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Employer;
import com.example.JFS_Job_Finding_Service.models.employer_type;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import com.example.JFS_Job_Finding_Service.repository.EmployerRepository;
import com.example.JFS_Job_Finding_Service.security.PasswordConfig;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil.*;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployerRepository employerRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    private PasswordEncoder passwordEncoder= PasswordConfig.passwordEncoder();
    private JwtUtil jwtUtil= new JwtUtil();
    public User getUserById(long id){
        return userRepository.findById(id).orElse(null);
    }
    public ResponseEntity<?> EmployerRegister(String email, String password, String confirmPass, String name, String employerType, Date dateOfBirth, String gender) {

        Map<String, Object> response = new HashMap<>();
        if (!password.equals(confirmPass)) {
            response.put("error", "no matching password");
            response.put("message", "Mật khẩu không khớp!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if(userRepository.findByEmail(email).isPresent()){
            response.put("error", "Duplicate email");
            response.put("message", "Email này đã được sử dụng bởi một tài khoản khác!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        User user=new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(name);
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender);
        user.setRole("Employer");
        try{
            userRepository.save(user);
        }catch (DataIntegrityViolationException e){
            response.put("error", "Email invalid");
            response.put("message", "Email không hợp lệ!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Employer employer=Employer.builder().user(user).type(employer_type.valueOf(employerType)).build();
        try {
            employerRepository.save(employer);
        }catch (DataIntegrityViolationException e){
            response.put("error", "something went wrong");
            response.put("message", "lỗi xảy ra khi tạo employer");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.put("status", "success");
        response.put("message", "Bạn đã đăng kí tài khoản thành công!");
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> EmployerLogin(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isEmpty()||employerRepository.findByUser(user.get()).isEmpty()){
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User does not exist");
            response.put("message", "Tài khoản không tồn tại!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(!passwordEncoder.matches(password,user.get().getPassword())){
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid password");
            response.put("message", "Sai mật khẩu!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(!user.get().isActive()){
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User is banned");
            response.put("message", "Tài khoản của bạn đã bị đình chỉ!");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        Optional<Employer> u=employerRepository.findByUser(user.get());
        String token=jwtUtil.generateToken(user.get().getEmail(),user.get().getRole());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("token", token);
        response.put("user", user.get());
        u.ifPresent(employer -> response.put("employer", employer));
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> ApplicantRegister(String email, String password, String confirmPass, String name, Date dateOfBirth, String gender) {

        Map<String, Object> response = new HashMap<>();
        if (!password.equals(confirmPass)) {
            response.put("error", "no matching password");
            response.put("message", "Mật khẩu không khớp!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if(userRepository.findByEmail(email).isPresent()){
            response.put("error", "Duplicate email");
            response.put("message", "Email này đã được sử dụng bởi một tài khoản khác!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        User user=new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(name);
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender);
        user.setRole("Applicant");
        try{
            userRepository.save(user);
        }catch (DataIntegrityViolationException e){
            response.put("error", "Email invalid");
            response.put("message", "Email không hợp lệ!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Applicant applicant=Applicant.builder().user(user).build();
        try {
            applicantRepository.save(applicant);
        }catch (DataIntegrityViolationException e){
            response.put("error", "something went wrong");
            response.put("message", "lỗi xảy ra khi tạo applicant");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.put("status", "success");
        response.put("message", "Bạn đã đăng kí tài khoản thành công!");
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> ApplicantLogin(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isEmpty()||applicantRepository.findByUser(user.get()).isEmpty()){
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User does not exist");
            response.put("message", "Tài khoản không tồn tại!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(!passwordEncoder.matches(password,user.get().getPassword())){
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid password");
            response.put("message", "Sai mật khẩu!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(!user.get().isActive()){
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User is banned");
            response.put("message", "Tài khoản của bạn đã bị đình chỉ!");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        Optional<Applicant> u=applicantRepository.findByUser(user.get());
        String token=jwtUtil.generateToken(user.get().getEmail(),user.get().getRole());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("token", token);
        response.put("user", user.get());
        u.ifPresent(applicant -> response.put("applicant", applicant));
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> UpdateResume(String token,String email, Map<String, Object> resume) {
        if(jwtUtil.validateToken(token, email)){
            User user=userRepository.findByEmail(email).get();
            Applicant applicant = applicantRepository.findByUser(user).get();
            if(applicant==null){
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Applicant not found");
                return ResponseEntity.ok(response);
            }
            applicant.setResume(resume);
            Map<String, Object> response = new HashMap<>();

            try {
                applicantRepository.save(applicant);
                response.put("status", "success");
                response.put("user", user);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
            }
            return ResponseEntity.ok(response);
        }
        else{
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Token invalid");
            return ResponseEntity.ok(response);
        }
    }
    public ResponseEntity<?> getProfile(String token, Long userId) {
        Map<String, Object> response = new HashMap<>();
        if(!jwtUtil.validateToken(token, jwtUtil.extractEmail(token))) {
            response.put("status", "fail");
            response.put("message", "Unauthorized access");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            response.put("status", "fail");
            response.put("message", "User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        response.put("phone", user.get().getPhone());
        response.put("address", user.get().getAddress());
        response.put("gender", user.get().getGender());
        response.put("date_of_birth", user.get().getDateOfBirth());
        response.put("avatar", user.get().getAvatarUrl());
        response.put("createdAt", user.get().getCreatedAt());
        response.put("email", user.get().getEmail());
        response.put("name", user.get().getFullName());

        if(user.get().getRole().equals("Applicant")){
            Optional<Applicant> applicant = applicantRepository.findByUser(user.get());
            if(applicant.isEmpty()){
                response.put("status", "fail");
                response.put("message", "Applicant not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            response.put("status", "success");
            response.put("role", "Applicant");
            response.put("applicantId", applicant.get().getId());
            response.put("resume", applicant.get().getResume());
            return ResponseEntity.ok(response);

        } else if(user.get().getRole().equals("Employer")){
            Optional<Employer> employer = employerRepository.findByUser(user.get());
            if(employer.isEmpty()){
                response.put("status", "fail");
                response.put("message", "Employer not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            response.put("status", "success");
            response.put("role", "Employer");
            response.put("employerId", employer.get().getId());
            response.put("organization", employer.get().getType());

            return ResponseEntity.ok(response);
        } else {
            response.put("status", "fail");
            response.put("message", "Invalid user role");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
    public ResponseEntity<?> updateProfile(String token, UpdateProfile dto) {
        boolean check=jwtUtil.validateToken(token);
        if(!check){
            Map<String, Object> response = new HashMap<>();
            response.put("status", "fail");
            response.put("message", "Unauthorized access");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        boolean isApplicant=jwtUtil.checkWhetherIsApplicant(token);
        Optional<User> user = userRepository.findByEmail(jwtUtil.extractEmail(token));
        if(user.isEmpty()){
            Map<String, Object> response = new HashMap<>();
            response.put("status", "fail");
            response.put("message", "User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        if(isApplicant){
            Applicant applicant = applicantRepository.findByUser(user.get()).orElseThrow(() -> new RuntimeException("Applicant not found"));
            applicant.getUser().setFullName(dto.getName());
            applicant.getUser().setPhone(dto.getPhoneNumber());
            applicant.getUser().setAddress(dto.getLocation());
            applicant.getUser().setDateOfBirth(dto.getDateOfBirth());
            applicant.setResume(dto.getResume());
            userRepository.save(user.get());
            applicantRepository.save(applicant);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            Employer employer = employerRepository.findByUser(user.get()).orElseThrow(() -> new RuntimeException("Employer not found"));
            employer.getUser().setFullName(dto.getName());
            employer.getUser().setPhone(dto.getPhoneNumber());
            employer.getUser().setAddress(dto.getLocation());
            employer.getUser().setDateOfBirth(dto.getDateOfBirth());
            employer.setType(employer_type.valueOf(dto.getEmployerType()));
            userRepository.save(user.get());
            employerRepository.save(employer);
            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            return ResponseEntity.ok(response);
        }
    }
    public ResponseEntity<?> checkEmail(String email) {
        Map<String, Object> response = new HashMap<>();
        if(userRepository.findByEmail(email).isPresent()){
            response.put("error", "Duplicate email");
            response.put("message", "Email này đã được sử dụng bởi một tài khoản khác!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
    public String updatePassword(String email, String oldPass, String password, String confirmPass) {
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isEmpty()){throw new RuntimeException("User does not exist");}
        if(!passwordEncoder.matches(oldPass,user.get().getPassword())){throw new RuntimeException("Invalid password");}
        if(!password.equals(confirmPass)){throw new RuntimeException("Passwords do not match");}
        user.get().setPassword(passwordEncoder.encode(password));
        userRepository.save(user.get());
        return "success";
    }
    public ResponseEntity<?> checkPassword(String token, String password) {
        Map<String, Object> response = new HashMap<>();
        String email=jwtUtil.extractEmail(token);
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isEmpty()){
            response.put("error", "User does not exist");
            response.put("message", "you have to login or register first");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(!passwordEncoder.matches(password,user.get().getPassword())){
            response.put("error", "Invalid password");
            response.put("message", "wrong password");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(!jwtUtil.validateToken(token,email)){
            response.put("error", "Invalid token");
            response.put("message", "the token is expired or invalid");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        response.put("status", "success");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    public String deleteUser(long id) {
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){throw new RuntimeException("User does not exist");}
        userRepository.delete(user.get());
        return "success";
    }
    public ResponseEntity<?> checkPermission(String token, String role) {
        Map<String, Object> response = new HashMap<>();
        boolean b=jwtUtil.checkPermission(token, role);
        if(b){
            response.put("result", "accept");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.put("result", "deny");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    public ResponseEntity<?> isTokenValid(String token, String email) {
        Map<String, Object> response = new HashMap<>();
        if(jwtUtil.validateToken(token,email)){
            response.put("result", "valid");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.put("result", "invalid");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
}
