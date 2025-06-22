package com.example.JFS_Job_Finding_Service.Controller;
import com.example.JFS_Job_Finding_Service.DTO.*;
import com.example.JFS_Job_Finding_Service.Services.AdminService;
import com.example.JFS_Job_Finding_Service.Services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;

    @PostMapping("/register/employer")
    @Operation(summary = "User Registration", description = "Register a new user with email, password, name, and role.")
    public ResponseEntity<?> EmployerRegister(@RequestBody EmployerRegisterRequest employerRegisterRequest) {
        return userService.EmployerRegister(
                employerRegisterRequest.getEmail(),
                employerRegisterRequest.getPassword(), employerRegisterRequest.getRetypePass(),
                employerRegisterRequest.getName(), employerRegisterRequest.getEmployerType(), employerRegisterRequest.getDateOfBirth(), employerRegisterRequest.getGender());
    }

    @PostMapping("/register/applicant")
    @Operation(summary = "User Registration", description = "Register a new user with email, password, name, and role.")
    public ResponseEntity<?> ApplicantRegister(@RequestBody ApplicantRegisterRequest applicantRegisterRequest) {
        return userService.ApplicantRegister(
                applicantRegisterRequest.getEmail(),
                applicantRegisterRequest.getPassword(), applicantRegisterRequest.getRetypePass(),
                applicantRegisterRequest.getName(), applicantRegisterRequest.getDateOfBirth(),applicantRegisterRequest.getGender());
    }

    @PostMapping("/login/employer")
    @Operation(summary = "User Login", description = "Authenticate a user and return a JWT token.")
    public ResponseEntity<?> EmployerLogin(@RequestBody LoginRequest loginRequest) {
        try{
            return userService.EmployerLogin(loginRequest.getEmail(),loginRequest.getPassword());
        }
        catch (RuntimeException e){
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
    @PostMapping("/login/applicant")
    @Operation(summary = "User Login", description = "Authenticate a user and return a JWT token.")
    public ResponseEntity<?> ApplicantLogin(@RequestBody LoginRequest loginRequest) {
        try{
            return userService.ApplicantLogin(loginRequest.getEmail(),loginRequest.getPassword());
        }
        catch (RuntimeException e){
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
    @PostMapping("/login/admin")
    @Operation(summary = "Admin Login", description = "Authenticate an admin and return a JWT token.")
    public ResponseEntity<?> AdminLogin(@RequestBody LoginRequest loginRequest) {
        try{
            return adminService.login(loginRequest.getEmail(),loginRequest.getPassword());
        }
        catch (RuntimeException e){
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
    @PostMapping("/register/admin")
    @Operation(summary = "Admin Registration", description = "Register a new admin with email, password, name, and secret pass.")
    public ResponseEntity<?> AdminRegister(@RequestBody Map<String, String> body) {
        String secretPass = body.get("secretPass");
        String fullName = body.get("fullName");
        String email = body.get("email");
        String password = body.get("password");
        return adminService.addAdmin(secretPass, fullName, email, password);
    }
    @PostMapping("/checkEmail")
    @Operation(summary = "check whether the email is already used")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, Object> body ) {
        System.out.println(body.get("email").toString());
        return userService.checkEmail(body.get("email").toString());
    }
    @PostMapping("/checkPass")
    @Operation(summary = "check whether the password is correct", description = "use token and password for checking process!")
    public ResponseEntity<?> checkPass(@RequestHeader HttpHeaders headers, @RequestBody CheckPassRequest checkPassRequest) {
        return (userService.checkPassword(headers.get("token").get(0).toString(),checkPassRequest.getPassword()));
    }
    @PostMapping("/checkPermission")
    @Operation(summary = "check whether the permission is right")
    public ResponseEntity<?> checkPermission(@RequestBody Map<String, Object> body ) {
        return userService.checkPermission(body.get("token").toString(),body.get("role").toString());
    }
    @PostMapping("/isTokenValid")
    @Operation(summary = "check whether the token is valid")
    public ResponseEntity<?> isTokenValid(@RequestHeader HttpHeaders headers ) {
        if(headers!=null && headers.get("authorization")!=null && headers.get("email")!=null){
            return userService.isTokenValid(headers.get("authorization").get(0).substring(7), headers.get("email").get(0));

        }else{
            return ResponseEntity.status(401).body("Token is null");
        }
    }
    @PostMapping("/profile")
    @Operation(summary = "get profile information")
    public ResponseEntity<?> getProfile(@RequestHeader HttpHeaders headers, @RequestParam("userId") Long userId) {
        if(headers!=null && headers.get("token")!=null){
            return userService.getProfile(headers.getFirst("token"), userId);
        }else{
            return ResponseEntity.status(401).body("Token is null");
        }
    }
    @PostMapping("/updateProfile")
    @Operation(summary = "update profile information")
    public ResponseEntity<?> updateProfile(@RequestHeader HttpHeaders headers, @RequestBody UpdateProfile body) {
        if(headers!=null && headers.get("token")!=null){
            return userService.updateProfile(headers.getFirst("token"), body);
        }else{
            return ResponseEntity.status(401).body("Token is null");
        }
    }
    @PostMapping("/updateAvatar")
    @Operation(summary = "update avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        if(headers!=null && headers.get("token")!=null){
            return userService.updateAvatar(headers.getFirst("token"), avatar);
        }else{
            return ResponseEntity.status(401).body("Token is null");
        }
    }
}
