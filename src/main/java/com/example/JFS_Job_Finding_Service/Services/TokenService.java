package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    public TokenService(JwtUtil jwtUtil, RedisTokenService redisTokenService) {
        this.jwtUtil = jwtUtil;
        this.redisTokenService = redisTokenService;
    }

    public boolean validateToken(String token, String email) {
        return jwtUtil.validateToken(token, email)&&!redisTokenService.isTokenBlacklisted(token);
    }
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token)&&!redisTokenService.isTokenBlacklisted(token);
    }
}
