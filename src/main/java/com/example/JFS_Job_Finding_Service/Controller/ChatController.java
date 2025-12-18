package com.example.JFS_Job_Finding_Service.Controller;

import com.example.JFS_Job_Finding_Service.Services.ChatService;
import com.example.JFS_Job_Finding_Service.Services.CloudinaryService;
import com.example.JFS_Job_Finding_Service.Services.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/signature")
    public ResponseEntity<?> getUploadSignature(@RequestHeader("token") String token) {
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalid");
        }
        return ResponseEntity.ok(cloudinaryService.generateClientSignature());
    }

    @GetMapping("/messages")
    @Operation(summary="take next 20 messages between two users")
    public ResponseEntity<?> getAllMessages(@RequestParam("senderId") long senderId, @RequestParam("receiverId") long receiverId, @RequestParam("page") int page,@RequestHeader HttpHeaders headers) {
        String token=headers.get("token").get(0).toString();
        return chatService.getAllMessages(senderId, receiverId, page);
    }
    @GetMapping("/conversations")
    @Operation(summary = "get friends")
    public ResponseEntity<?> getAllConversations(@RequestHeader HttpHeaders headers, @RequestParam Long userId) {
        String token=headers.get("token").get(0).toString();
        return ResponseEntity.ok().body(chatService.getInboxList(userId));
    }
}
