package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.Services.AgoraTokenService;
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
    public Map<String, String> getToken(@RequestParam("channel") String channel, @RequestParam("uid") int uid) {
        String token = tokenService.generateToken(channel, uid);
        return Map.of("token", token);
    }
}