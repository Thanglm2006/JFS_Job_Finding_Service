package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.Services.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping("/users")
    @Operation(summary="take all users chated with")
    public ResponseEntity<?> getAllUsersChatedWith(@RequestParam long senderId, @RequestHeader HttpHeaders headers) {
        String token=headers.get("token").get(0).toString();
        return chatService.getAllUsersChatedWith(senderId);
    }
    @PostMapping("/messages")
    @Operation(summary="take next 20 messages between two users")
    public ResponseEntity<?> getAllMessages(@RequestParam long senderId, @RequestParam long receiverId, @RequestParam int page,@RequestHeader HttpHeaders headers) {
        String token=headers.get("token").get(0).toString();
        return chatService.getAllMessages(senderId, receiverId, page);
    }
}
