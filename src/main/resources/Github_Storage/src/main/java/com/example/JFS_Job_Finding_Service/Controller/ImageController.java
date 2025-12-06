package com.example.JFS_Job_Finding_Service.Controller;
import com.example.JFS_Job_Finding_Service.Services.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    @Autowired
    private ImgurService imgurService;

    @PostMapping("/upload")
    @Operation(summary = "Upload an image to Imgur")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = imgurService.uploadImage(file);
            return ResponseEntity.ok(imageUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
    @PostMapping("/upload-multiple")
    @Operation(summary = "Upload multiple images to Imgur")
    public ResponseEntity<List<String>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        try {
            List<String> imageUrls = imgurService.uploadImages(files);
            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(500).body(List.of("Upload failed: " + e.getMessage()));
        }
    }
    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload-cloudinary")
    public ResponseEntity<String> upload(@RequestParam("files") MultipartFile[] files,@RequestParam("folder") String folder) {
        try {
            String url = cloudinaryService.uploadFiles(files,folder);

            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}

