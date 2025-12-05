package com.example.JFS_Job_Finding_Service.Services;

import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AgoraTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AgoraTokenService.class);

    @Value("${spring.agora.id}")
    private String APP_ID;

    @Value("${spring.agora.cer}")
    private String APP_CERTIFICATE;

    private static final int EXPIRATION_TIME_IN_SECONDS = 3600; // 1 hour

    public String generateToken(String channelName, int uid) {
        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();

        int timestamp = (int) (System.currentTimeMillis() / 1000 + EXPIRATION_TIME_IN_SECONDS);

        String token = tokenBuilder.buildTokenWithUid(
                APP_ID,
                APP_CERTIFICATE,
                channelName,
                uid,
                Role.ROLE_PUBLISHER,
                timestamp, // privilegeExpiration:
                timestamp  // tokenExpiration:
        );

        logger.info("Generated Agora Token for Channel: {}, UID: {}", channelName, uid);

        return token;
    }
}