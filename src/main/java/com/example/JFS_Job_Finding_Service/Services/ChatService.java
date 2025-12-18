package com.example.JFS_Job_Finding_Service.Services;

import com.example.JFS_Job_Finding_Service.DTO.Chat.InboxItemDTO;
import com.example.JFS_Job_Finding_Service.DTO.Chat.message;
import com.example.JFS_Job_Finding_Service.models.ChatMessage;
import com.example.JFS_Job_Finding_Service.models.User;
import com.example.JFS_Job_Finding_Service.repository.ChatMessageRepository;
import com.example.JFS_Job_Finding_Service.repository.UserRepository;
import com.example.JFS_Job_Finding_Service.ultils.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MqttPahoClientFactory mqttClientFactory;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenService tokenService;

    private final Map<String, String> presenceMap = new ConcurrentHashMap<>();
    private MessageHandler mqttOutboundHandler;

    // --- KHỞI TẠO OUTBOUND HANDLER (ĐỂ GỬI TIN) ---
    @PostConstruct
    public void init() {
        mqttOutboundHandler = new MqttPahoMessageHandler("backend-outbound", mqttClientFactory);
        ((MqttPahoMessageHandler) mqttOutboundHandler).setDefaultQos(1);
        ((MqttPahoMessageHandler) mqttOutboundHandler).setDefaultRetained(false);
    }

    // --- 1. XỬ LÝ LƯU & GỬI TIN NHẮN ---
    @Transactional
    public ChatMessage saveMessage(long senderId, long recipientId, String content, String fileUrl) {
        try {
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Sender not found ID: " + senderId));
            User recipient = userRepository.findById(recipientId)
                    .orElseThrow(() -> new RuntimeException("Recipient not found ID: " + recipientId));

            ChatMessage chatMessage = new ChatMessage(sender, recipient, content, fileUrl);
            chatMessage.setTimestamp(LocalDateTime.now());

            return chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            System.err.println("Error saving message: " + e.getMessage());
            throw e;
        }
    }

    @ServiceActivator(inputChannel = "chatChannel")
    public void handleChatMessage(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = (String) message.getPayload();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);

            // Chặn tin nhắn do chính server gửi ra (tránh loop)
            if (!jsonNode.has("token")) return;

            String token = jsonNode.get("token").asText();
            String email = jwtUtil.extractEmail(token);

            if (!tokenService.validateToken(token, email)) {
                System.err.println("Chat rejected: Invalid Token " + email);
                return;
            }

            User sender = userRepository.findByEmail(email).orElse(null);
            if (sender == null) return;
            Long senderId = sender.getId();

            Long recipientId = Long.parseLong(jsonNode.get("recipient").asText());
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "text";
            String content = jsonNode.has("content") ? jsonNode.get("content").asText() : "";

            String roomId = getRoomId(senderId, recipientId);
            if (!topic.endsWith(roomId)) return;

            // Lưu DB
            ChatMessage savedMsg;
            if ("image".equalsIgnoreCase(type)) {
                savedMsg = saveMessage(senderId, recipientId, "", content);
            } else {
                savedMsg = saveMessage(senderId, recipientId, content, null);
            }

            // Phản hồi lại Client (Publish)
            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("type", type);
            responsePayload.put("sender", senderId);
            responsePayload.put("recipient", recipientId);
            responsePayload.put("content", content);
            responsePayload.put("timestamp", savedMsg.getTimestamp().toString());

            publishToTopic("/chat/" + roomId, mapper.writeValueAsString(responsePayload), true);

        } catch (Exception e) {
            System.err.println("Exception in handleChatMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- 2. XỬ LÝ REQUEST DANH SÁCH BẠN BÈ ---
    @ServiceActivator(inputChannel = "requestChannel")
    public void handleUserRequest(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = (String) message.getPayload();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);

            if (!jsonNode.has("token")) return;
            String token = jsonNode.get("token").asText();
            String email = jwtUtil.extractEmail(token);

            if (!tokenService.validateToken(token, email)) return;

            User requestUser = userRepository.findByEmail(email).orElse(null);
            if (requestUser == null) return;

            String userIdParam = topic.split("/")[3];
            Long requestedUserId = Long.parseLong(userIdParam);

            if (!requestUser.getId().equals(requestedUserId)) return;

            List<InboxItemDTO> inbox = getInboxList(requestedUserId);

            Map<String, Object> response = Map.of(
                    "type", "InboxList",
                    "content", mapper.writeValueAsString(inbox),
                    "sender", userIdParam
            );

            publishToTopic("/user/" + userIdParam + "/private", mapper.writeValueAsString(response), false);
        } catch (Exception e) {
            System.err.println("Error in handleUserRequest: " + e.getMessage());
        }
    }

    // --- 3. XỬ LÝ TRẠNG THÁI ONLINE ---
    @ServiceActivator(inputChannel = "presenceChannel")
    public void handlePresence(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String status = (String) message.getPayload();

            String[] parts = topic.split("/");
            if (parts.length < 3) return;

            String userId = parts[2];
            presenceMap.put(userId, status);
        } catch (Exception e) {
            System.err.println("Error in handlePresence: " + e.getMessage());
        }
    }

    // --- UTILS & API ---

    public ResponseEntity<?> getAllMessages(long currentUserId, long partnerId, int page) {
        if (!userRepository.existsById(currentUserId) || !userRepository.existsById(partnerId)) {
            return ResponseEntity.badRequest().body("User not found");
        }

        Pageable pageable = PageRequest.of(page, 20);
        org.springframework.data.domain.Page<ChatMessage> pageResult =
                chatMessageRepository.findConversation(currentUserId, partnerId, pageable);

        List<message> msgList = pageResult.getContent().stream().map(chatMsg -> {
            boolean isMyMessage = chatMsg.getSender().getId() == currentUserId;
            String type = (chatMsg.getFileUrl() != null && !chatMsg.getFileUrl().isEmpty()) ? "Image" : "Text";
            return new message(
                    chatMsg.getMessage(),
                    isMyMessage,
                    chatMsg.getTimestamp(),
                    type,
                    chatMsg.getFileUrl()
            );
        }).collect(Collectors.toList());


        Map<String, Object> response = new HashMap<>();
        response.put("data", msgList);
        response.put("currentPage", pageResult.getNumber());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("totalMessages", pageResult.getTotalElements());
        response.put("hasNext", pageResult.hasNext());

        return ResponseEntity.ok(response);
    }

    public List<InboxItemDTO> getInboxList(long myId) {
        List<ChatMessageRepository.InboxProjection> projections = chatMessageRepository.findInboxList(myId);

        return projections.stream().map(p -> {
            String displayContent;
            if (p.getFileUrl() != null && !p.getFileUrl().isEmpty()) {
                boolean didISend = p.getLastSenderId() == myId;
                displayContent = didISend ? "Bạn đã gửi một ảnh" : "Đã gửi một ảnh";
            } else {
                displayContent = p.getLastMessage();
            }

            return new InboxItemDTO(
                    p.getUserId(),
                    p.getFullName(),
                    p.getAvatarUrl(),
                    presenceMap.getOrDefault(String.valueOf(p.getUserId()), "Offline"),
                    displayContent,
                    p.getLastMessageTime().toLocalDateTime(),
                    true
            );
        }).collect(Collectors.toList());
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