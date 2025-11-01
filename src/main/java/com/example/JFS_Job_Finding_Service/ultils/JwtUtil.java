package com.example.JFS_Job_Finding_Service.ultils;

import com.example.JFS_Job_Finding_Service.Services.UserService;
import com.example.JFS_Job_Finding_Service.models.Applicant;
import com.example.JFS_Job_Finding_Service.models.Employer;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.ApplicantRepository;
import com.example.JFS_Job_Finding_Service.repository.EmployerRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
@Component
@ComponentScan
public class JwtUtil {
    @Autowired
    private EmployerRepository employerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicantRepository applicantRepository;

    public JwtUtil() {
    }

    private final String SECRET_KEY = "tfcyhdfhvbnjkftrygubhijutjyvhbnklyurfdvgbiujoasfsdf";
    private final long EXPIRATION_TIME = 10800000; // 3 hours

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(String email,String role) {

        return Jwts.builder()
                .setSubject(email+"|"+role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public String extractEmail(String token) {
        String subject=Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().getSubject();
        int idx=subject.indexOf("|");
        return subject.substring(0,idx);
    }
    public boolean validateToken(String token) {
        return !isTokenExpired(token) && extractEmail(token) != null;
    }
    public String extractSubject(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    public Employer getEmployer(String token) {
        String email = extractEmail(token);
        User user;
        user=userRepository.findByEmail(email).get();
        Employer employer = employerRepository.findByUser(user).orElse(null);
        if (employer == null) {
            System.out.println("Employer not found for email: " + email);
            return null;
        }
        return employer;
    }
    public Applicant getApplicant(String token) {
        String email = extractEmail(token);
        User user;
        user=userRepository.findByEmail(email).get();
        Applicant applicant= applicantRepository.findByUser(user).orElse(null);
        if (applicant == null) {
            System.out.println("Applicant not found for email: " + email);
            return null;
        }
        return applicant;
    }
    //check permission
    public boolean checkPermission(String token, String role) {
        String subject=extractSubject(token);
        int idx=subject.indexOf("|");
        String permission=subject.substring(idx+1);
        return role.equals(permission);
    }
    //check whether is an employer
    public boolean checkWhetherIsEmployer(String token) {
        String subject=extractSubject(token);
        int idx=subject.indexOf("|");
        String role=subject.substring(idx+1);
        return role.equals("Employer");
    }
    //check whether is an applicant
    public boolean checkWhetherIsApplicant(String token) {
        String subject=extractSubject(token);
        int idx=subject.indexOf("|");
        String role=subject.substring(idx+1);
        return role.equals("Applicant");
    }
    // ✅ Check if Token is Valid
    public boolean validateToken(String token, String userEmail) {
        String trueEmail=extractEmail(token);
        System.out.println("trueEmail: "+trueEmail);
        return trueEmail.equals(userEmail) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
}
