package com.example.JFS_Job_Finding_Service.Controller;
import com.example.JFS_Job_Finding_Service.DTO.ApplicantRegisterRequest;
import com.example.JFS_Job_Finding_Service.DTO.CheckPassRequest;
import com.example.JFS_Job_Finding_Service.DTO.LoginRequest;
import com.example.JFS_Job_Finding_Service.DTO.EmployerRegisterRequest;
import com.example.JFS_Job_Finding_Service.Services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/register/employer")
    @Operation(summary = "User Registration", description = "Register a new user with email, password, name, and role.")
    public ResponseEntity<?> EmployerRegister(@RequestBody EmployerRegisterRequest employerRegisterRequest) {
        System.out.println(employerRegisterRequest.toString());
        return userService.EmployerRegister(
                employerRegisterRequest.getEmail(),
                employerRegisterRequest.getPassword(), employerRegisterRequest.getRetypePass(),
                employerRegisterRequest.getName(), employerRegisterRequest.getEmployerType());
    }

    @PostMapping("/register/applicant")
    @Operation(summary = "User Registration", description = "Register a new user with email, password, name, and role.")
    public ResponseEntity<?> ApplicantRegister(@RequestBody ApplicantRegisterRequest applicantRegisterRequest) {
        return userService.ApplicantRegister(
                applicantRegisterRequest.getEmail(),
                applicantRegisterRequest.getPassword(), applicantRegisterRequest.getRetypePass(),
                applicantRegisterRequest.getName(), applicantRegisterRequest.getResume());
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
}
