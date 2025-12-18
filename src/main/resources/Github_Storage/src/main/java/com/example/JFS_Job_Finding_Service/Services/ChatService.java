package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Chat.message;
import com.example.JFS_Job_Finding_Service.DTO.Chat.userInChat;
import com.example.JFS_Job_Finding_Service.models.ChatMessage;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.ChatMessageRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private UserRepository userRepository;
    public void saveMessage(long senderId, long recipientId, String message) {
         User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
         User recipient = userRepository.findById(recipientId).orElseThrow(() -> new RuntimeException("Recipient not found"));
        chatMessageRepository.save(new ChatMessage(sender, recipient, message));
    }
    public ResponseEntity<?> getAllMessages(long senderId, long receiverId, int page) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("User not found"));
        List<ChatMessage> messagesFromSenderToReceiver = chatMessageRepository.findMessagesFromSenderToReceiver(sender, receiver, PageRequest.of(page, 20));
        List<ChatMessage> messagesFromReceiverToSender = chatMessageRepository.findMessagesFromSenderToReceiver(receiver, sender, PageRequest.of(page, 20));
        List<message> MessageFromSenderToReceiver = new ArrayList<>();
        List<message> MessageFromReceiverToSender = new ArrayList<>();
        messagesFromSenderToReceiver.forEach(chatMessage -> {
            message msg = new message(chatMessage.getMessage(), chatMessage.getTimestamp(),"From");
            MessageFromSenderToReceiver.add(msg);
        });
        messagesFromReceiverToSender.forEach(chatMessage -> {
            message msg = new message(chatMessage.getMessage(), chatMessage.getTimestamp(),"To");
            MessageFromReceiverToSender.add(msg);
        });

        HashMap<String, List<message>> response = new HashMap<>();
        response.put("messagesFrom", MessageFromSenderToReceiver);
        response.put("messagesTo", MessageFromReceiverToSender);
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> getAllUsersChatedWith(long senderId) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("User not found"));
        List<User> users = chatMessageRepository.findReceiversBySender(sender);
        List<User> users2 = chatMessageRepository.findSendersByReceiver(sender);
        users.addAll(users2);
        Set<User> uniqueUsers = new HashSet<>(users);
        uniqueUsers.remove(sender);
        users.clear();
        users.addAll(uniqueUsers);
        List<userInChat> res= new ArrayList<>();
        users.forEach(user->{
            userInChat userInChat = new userInChat(user.getId(), user.getFullName(),user.getAvatarUrl());
            res.add(userInChat);
        });
        return ResponseEntity.ok(res);
    }
}
