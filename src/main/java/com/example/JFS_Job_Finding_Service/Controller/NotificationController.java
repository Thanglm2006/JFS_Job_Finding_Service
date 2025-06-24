package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.Services.NotificationService;
import com.example.JFS_Job_Finding_Service.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/notification")
public class NotificationController {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @GetMapping("/getAllNotifications")
    public Object getAllNotifications(@RequestHeader HttpHeaders headers) {
        return notificationService.getAllNotifications(headers.getFirst("token"));
    }
}
