package com.example.JFS_Job_Finding_Service.config;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dqkxxaulc",
                "api_key", "861831694676724",
                "api_secret", "vz2M6CYBdjHHI0vPtJcN1cq8XAI"));
    }
}
