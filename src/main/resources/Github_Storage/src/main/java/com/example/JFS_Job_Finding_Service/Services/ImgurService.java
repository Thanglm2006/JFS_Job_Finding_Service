package com.example.JFS_Job_Finding_Service.Services;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.io.IOException;

@Service
public class ImgurService {
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";
    private static final String CLIENT_ID = "06eb31d8f5a18ca";
    private static final String CLIENT_SECRET ="af103271954188d2026510b07bde1edee6e9019c";
    public String uploadImage(MultipartFile file) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        // Convert file to Base64
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Client-ID " + CLIENT_ID);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("image", base64Image);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        // Send request to Imgur API
        ResponseEntity<Map> response = restTemplate.exchange(IMGUR_UPLOAD_URL, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            return (String) data.get("link"); // Return image URL
        } else {
            throw new RuntimeException("Failed to upload image");
        }
    }
    public List<String> uploadImages(MultipartFile[] files) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            imageUrls.add(uploadImage(file));
        }
        return imageUrls;
    }
}
