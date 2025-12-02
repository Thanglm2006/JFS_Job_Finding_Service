package com.example.JFS_Job_Finding_Service.Services;
import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgoraTokenService {
    @Value("${spring.agora.id}")
    private String APP_ID;
    @Value("${spring.agora.cer}")
    private  String APP_CERTIFICATE;
    private static final int EXPIRATION_TIME = 3600; // 1 hour

    public String generateToken(String channelName, int uid) {
        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
        long timestamp = (System.currentTimeMillis() / 1000) + EXPIRATION_TIME;

        // Role.ROLE_PUBLISHER = User can talk/stream video
        // Role.ROLE_SUBSCRIBER = User can only watch
        String token=tokenBuilder.buildTokenWithUid(
                APP_ID,
                APP_CERTIFICATE,
                channelName,
                uid,
                Role.ROLE_PUBLISHER,
                (int) timestamp,
                (int) timestamp
        );
        System.out.println(APP_ID);
        System.out.println(APP_CERTIFICATE);
        System.out.println("token:"+token);
        return token;
    }
}