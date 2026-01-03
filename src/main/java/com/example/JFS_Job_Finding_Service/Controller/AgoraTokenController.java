package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.Services.AgoraTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.token.TokenService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AgoraTokenController {

    private final AgoraTokenService tokenService;

    public AgoraTokenController (AgoraTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/call-token")
    public ResponseEntity<?> getToken(@RequestHeader HttpHeaders headers, @RequestParam("channel") String channel, @RequestParam("uid") int uid) {
        String token = tokenService.generateToken(headers.getFirst("token"),channel, uid);
        if(token!=null)
        return ResponseEntity.ok().body(Map.of("token",token));
        else return ResponseEntity.badRequest().build();
    }
}