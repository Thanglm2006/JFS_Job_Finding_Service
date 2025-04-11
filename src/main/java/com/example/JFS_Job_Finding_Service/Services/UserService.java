package com.example.JFS_Job_Finding_Service.Services;
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
    public ResponseEntity<?> EmployerRegister(String email, String password, String confirmPass, String name, String employerType) {

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
        Optional<Employer> u=employerRepository.findByUser(user.get());
        System.out.println(u.get().getId());
        String token=jwtUtil.generateToken(user.get().getEmail(),user.get().getRole());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("token", token);
        response.put("user", user.get());
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> ApplicantRegister(String email, String password, String confirmPass, String name, Map<String, Object> resume) {

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
        user.setRole("Applicant");
        try{
            userRepository.save(user);
        }catch (DataIntegrityViolationException e){
            response.put("error", "Email invalid");
            response.put("message", "Email không hợp lệ!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Applicant applicant=Applicant.builder().user(user).resume(resume).build();
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
        Optional<Applicant> u=applicantRepository.findByUser(user.get());
        System.out.println(u.get().getId()+"\n"+u.get().getResume());
        String token=jwtUtil.generateToken(user.get().getEmail(),user.get().getRole());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("token", token);
        response.put("user", user.get());
        return ResponseEntity.ok(response);
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
