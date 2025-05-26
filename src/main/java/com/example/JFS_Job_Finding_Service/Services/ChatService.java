package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.message;
import com.example.JFS_Job_Finding_Service.DTO.userInChat;
import com.example.JFS_Job_Finding_Service.models.ChatMessage;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.ChatMessageRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    public void saveImage(long senderId, long recipientId, String fileUrl) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
        User recipient = userRepository.findById(recipientId).orElseThrow(() -> new RuntimeException("Recipient not found"));
        ChatMessage chatMessage = new ChatMessage(sender, recipient," ", fileUrl);
        chatMessageRepository.save(chatMessage);
    }
    public ResponseEntity<?> getAllMessages(long senderId, long receiverId, int page) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("User not found"));
        List<ChatMessage> messagesFromSenderToReceiver = chatMessageRepository.findMessagesFromSenderToReceiver(sender, receiver, PageRequest.of(page, 20));
        List<ChatMessage> messagesFromReceiverToSender = chatMessageRepository.findMessagesFromSenderToReceiver(receiver, sender, PageRequest.of(page, 20));
        List<message> MessageFromSenderToReceiver = new ArrayList<>();
        List<message> MessageFromReceiverToSender = new ArrayList<>();
        messagesFromSenderToReceiver.forEach(chatMessage -> {
            message msg;
            if(chatMessage.getFileUrl()==null)msg= new message(chatMessage.getMessage(),true,LocalDateTime.ofInstant(chatMessage.getTimestamp(), ZoneId.of("Asia/Ho_Chi_Minh")),"Text",chatMessage.getFileUrl());
            else msg= new message(chatMessage.getMessage(),true,LocalDateTime.ofInstant(chatMessage.getTimestamp(), ZoneId.of("Asia/Ho_Chi_Minh")),"Image",chatMessage.getFileUrl());
            MessageFromSenderToReceiver.add(msg);
        });
        messagesFromReceiverToSender.forEach(chatMessage -> {
            message msg;
            if(chatMessage.getFileUrl()==null) msg=new message(chatMessage.getMessage(),false,LocalDateTime.ofInstant(chatMessage.getTimestamp(), ZoneId.of("Asia/Ho_Chi_Minh")),"Text",chatMessage.getFileUrl());
            else msg=new message(chatMessage.getMessage(),false,LocalDateTime.ofInstant(chatMessage.getTimestamp(), ZoneId.of("Asia/Ho_Chi_Minh")),"Image",chatMessage.getFileUrl());
            MessageFromReceiverToSender.add(msg);
        });

        HashMap<String, List<message>> response = new HashMap<>();
        response.put("messagesFrom", MessageFromSenderToReceiver);
        response.put("messagesTo", MessageFromReceiverToSender);
        return ResponseEntity.ok(response);
    }
    public List<userInChat> getAllUsersChatedWith(long userId) {
        User sender = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<ChatMessage> ress= chatMessageRepository.findAll();
        List<userInChat> u= new ArrayList<>();
        for(ChatMessage m:ress){
            userInChat userInChat = new userInChat(m.getSender().getId(), m.getSender().getFullName(), m.getSender().getAvatarUrl());
            if(!u.contains(userInChat)){
                u.add(userInChat);
            }
        }
        if(u.isEmpty()){
            System.out.println(u);
        }
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
        return res;
    }
}
