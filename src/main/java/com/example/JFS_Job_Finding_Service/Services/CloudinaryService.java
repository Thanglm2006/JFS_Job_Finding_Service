package com.example.JFS_Job_Finding_Service.Services;

import com.cloudinary.*;
import com.cloudinary.http44.*;
import com.cloudinary.utils.*;
import com.example.JFS_Job_Finding_Service.models.ImageFolders;
import com.example.JFS_Job_Finding_Service.repository.ImageFoldersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Service
public class CloudinaryService {
    @Autowired
    private Cloudinary cloudinary;
    @Autowired
    private ImageFoldersRepository imageFoldersRepository;

    public String uploadFiles(MultipartFile[] files, String folderName) throws IOException {

        ArrayList<String> urls = new ArrayList<>();
        for(MultipartFile file : files){
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", folderName));
            urls.add(uploadResult.get("secure_url").toString());
            imageFoldersRepository.save(new ImageFolders(folderName, uploadResult.get("secure_url").toString()));
        }
        return folderName;
    }
    public String uploadFile(File file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap("folder", "messageIMGs"));
        return uploadResult.get("secure_url").toString();
    }
    public String uploadFile(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", "messageIMGs"));
        return uploadResult.get("secure_url").toString();
    }
}
