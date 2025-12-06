package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisTokenService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtUtil jwtUtil; // Your existing Utils class

    private static final String BLACKLIST_PREFIX = "BLACKLIST_";

    public ResponseEntity<?> blacklistToken(String token) {
        String email = jwtUtil.extractEmail(token);
        Date expirationDate = jwtUtil.extractExpiration(token);
        long timeToLive = expirationDate.getTime() - System.currentTimeMillis();

        if (timeToLive > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    email,
                    timeToLive,
                    TimeUnit.MILLISECONDS
            );
        }
        return ResponseEntity.ok().build();
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}

