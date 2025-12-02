package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.message;
import com.example.JFS_Job_Finding_Service.DTO.userInChat;
import com.example.JFS_Job_Finding_Service.models.ChatMessage;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.ChatMessageRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private MqttPahoClientFactory mqttClientFactory;
    private final Map<String, String> presenceMap = new ConcurrentHashMap<>();  // userId -> "Online"/"Offline"

    private MqttPahoMessageDrivenChannelAdapter chatAdapter;
    private MqttPahoMessageDrivenChannelAdapter presenceAdapter;
    private MqttPahoMessageDrivenChannelAdapter requestAdapter;

    private MessageHandler mqttOutboundHandler;

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
    public List<userInChat> getAllUsersChattedWith(long userId) {
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

    @PostConstruct
    public void init() {
        // Outbound handler for publishing2
        mqttOutboundHandler = new MqttPahoMessageHandler("backend-outbound", mqttClientFactory);
        ((MqttPahoMessageHandler) mqttOutboundHandler).setDefaultQos(1);
        ((MqttPahoMessageHandler) mqttOutboundHandler).setDefaultRetained(false);

        // Inbound for chat messages (/chat/#)
        chatAdapter = new MqttPahoMessageDrivenChannelAdapter("backend-chat-in", mqttClientFactory, "/chat/#");
        chatAdapter.setConverter(new DefaultPahoMessageConverter());
        chatAdapter.setQos(1);
        chatAdapter.setOutputChannel(chatChannel());  // Define channels as @Beans if needed
        chatAdapter.start();

        // Inbound for presence (/presence/#)
        presenceAdapter = new MqttPahoMessageDrivenChannelAdapter("backend-presence-in", mqttClientFactory, "/presence/#");
        presenceAdapter.setConverter(new DefaultPahoMessageConverter());
        presenceAdapter.setQos(1);
        presenceAdapter.setOutputChannel(presenceChannel());
        presenceAdapter.start();

        // Inbound for user list requests (/request/users/#)
        requestAdapter = new MqttPahoMessageDrivenChannelAdapter("backend-request-in", mqttClientFactory, "/request/users/#");
        requestAdapter.setConverter(new DefaultPahoMessageConverter());
        requestAdapter.setQos(1);
        requestAdapter.setOutputChannel(requestChannel());
        requestAdapter.start();
    }

    // Define channels (can be @Bean in config)
    private MessageChannel chatChannel() {
        return new org.springframework.integration.channel.DirectChannel();
    }

    private MessageChannel presenceChannel() {
        return new org.springframework.integration.channel.DirectChannel();
    }

    private MessageChannel requestChannel() {
        return new org.springframework.integration.channel.DirectChannel();
    }

    @ServiceActivator(inputChannel = "chatChannel")
    public void handleChatMessage(Message<?> message) throws Exception {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = (String) message.getPayload();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(payload);

        String type = jsonNode.get("type").asText();
        Long senderId = Long.parseLong(jsonNode.get("sender").asText());
        Long recipientId = Long.parseLong(jsonNode.get("recipient").asText());
        String content = jsonNode.get("content").asText();

        String roomId = getRoomId(senderId, recipientId);

        if (!topic.endsWith(roomId)) return;  // Validate topic

        switch (type) {
            case "text":
                saveMessage(senderId, recipientId, content);
                break;
            case "image":
                File tempFile = null;
                try {
                    String base64Data = content.split(",")[1];
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    Path tempFilePath = Files.createTempFile("upload-", Instant.now().toString());
                    Files.write(tempFilePath, imageBytes);
                    tempFile = tempFilePath.toFile();
                    String url = cloudinaryService.uploadFile(tempFile);
                    saveImage(senderId, recipientId, url);
                    // Republish with URL
                    Map<String, Object> response = Map.of(
                            "type", "Image",
                            "sender", senderId.toString(),
                            "content", url
                    );
                    publishToTopic("/chat/" + roomId, mapper.writeValueAsString(response), true);
                } finally {
                    if (tempFile != null) tempFile.delete();
                }
                return;  // Don't republish original base64
        }

        // Republish original for text
        publishToTopic("/chat/" + roomId, payload, true);
    }

    @ServiceActivator(inputChannel = "presenceChannel")
    public void handlePresence(Message<?> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String status = (String) message.getPayload();
        String userId = topic.split("/")[2];  // /presence/{userId}
        presenceMap.put(userId, status);
        System.out.println(userId + " is now " + status);
    }

    @ServiceActivator(inputChannel = "requestChannel")
    public void handleUserRequest(Message<?> message) throws Exception {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String userId = topic.split("/")[3];  // /request/users/{userId}
        Long userID = Long.parseLong(userId);

        List<userInChat> users = getAllUsersChattedWith(userID);
        for (userInChat u : users) {
            u.setStatus(presenceMap.getOrDefault(String.valueOf(u.getId()), "Offline"));
        }

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(users);
        Map<String, Object> response = Map.of(
                "type", "Users",
                "content", json,
                "sender", userId
        );

        publishToTopic("/user/" + userId + "/private", mapper.writeValueAsString(response), false);
    }

    private void publishToTopic(String topic, String payload, boolean retain) {
        Message<String> msg = org.springframework.integration.support.MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .setHeader("mqtt_qos", 1)
                .setHeader("mqtt_retained", retain)
                .build();
        mqttOutboundHandler.handleMessage(msg);
    }

    private String getRoomId(Long user1, Long user2) {
        return user1 < user2 ? user1 + "-" + user2 : user2 + "-" + user1;
    }
}
