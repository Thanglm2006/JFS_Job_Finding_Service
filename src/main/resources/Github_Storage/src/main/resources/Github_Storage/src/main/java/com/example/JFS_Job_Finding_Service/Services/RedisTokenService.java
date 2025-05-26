package com.example.JFS_Job_Finding_Service.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisTokenService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:"; // Prefix for blacklist keys

    // ❌ Add token to Redis blacklist with expiration time
    public void blacklistToken(String token, long expirationSeconds) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted", expirationSeconds, TimeUnit.SECONDS);
    }

    // ✅ Check if token is blacklisted
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}

