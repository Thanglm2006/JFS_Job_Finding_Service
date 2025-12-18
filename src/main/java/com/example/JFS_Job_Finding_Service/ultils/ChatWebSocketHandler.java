//package com.example.JFS_Job_Finding_Service.ultils;
//
//import com.example.JFS_Job_Finding_Service.DTO.Chat.userInChat;
//import com.example.JFS_Job_Finding_Service.Services.ChatService;
//import com.example.JFS_Job_Finding_Service.Services.CloudinaryService;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.time.Instant;
//import java.util.Base64;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//public class ChatWebSocketHandler extends TextWebSocketHandler {
////this is for saving, we use mqtt instead of these
//    @Autowired
//    private ChatService chatService;
//
//    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
//    @Autowired
//    private CloudinaryService cloudinaryService;
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        String id = getId(session);
//        sessions.put(id, session);
//        System.out.println(id + " connected");
//        long userID= Long.parseLong(id);
//        List<userInChat> users= chatService.getAllUsersChattedWith(userID);
//        for(userInChat u:users){
//            WebSocketSession userSession = sessions.get(String.valueOf(u.getId()));
//            if (userSession != null && userSession.isOpen()) {
//                u.setStatus("Online");
//            } else {
//                u.setStatus("Offline");
//            }
//        }
//        ObjectMapper mapper = new ObjectMapper();
//        String json = mapper.writeValueAsString(users);
//        session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
//                "type","Users",
//                "content", json,
//                "sender", id
//        ))
//        ));
//    }
//    @Override
//    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode jsonNode = mapper.readTree(message.getPayload());
//
//        String type = jsonNode.get("type").asText();
//        String recipient = jsonNode.get("recipient").asText();
//        String content = jsonNode.get("content").asText();
//        String senderId = getId(session);
//        WebSocketSession recipientSession = sessions.get(recipient);
//
//        switch (type) {
//            case "text":
//                chatService.saveMessage(Long.parseLong(senderId), Long.parseLong(recipient), content);
//                if (recipientSession != null && recipientSession.isOpen()) {
//                    recipientSession.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
//                            "type", "Text",
//                            "sender", senderId,
//                            "content", content
//                    ))));
//                }
//                break;
//
//            case "image":
//                File tempFile = null;
//                try {
//                    String base64Data = content.split(",")[1];
//                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
//                    // Write to a temporary file
//                    Path tempFilePath = Files.createTempFile("upload-", Instant.now().toString());
//                    Files.write(tempFilePath, imageBytes);
//                    tempFile = tempFilePath.toFile();
//                    String url = cloudinaryService.uploadFile(tempFile);
//                    chatService.saveImage(Long.parseLong(senderId), Long.parseLong(recipient), url);
//                    if (recipientSession != null && recipientSession.isOpen()) {
//                        recipientSession.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
//                                "type", "Image",
//                                "sender", senderId,
//                                "content", url
//                        ))));
//                    }
//                } catch (IOException | NumberFormatException e) {
//                    throw new RuntimeException(e);
//                }
//                tempFile.delete();
//                break;
//            default:
//                System.out.println("Unknown message type: " + type);
//        }
//    }
//    private String getId(WebSocketSession session) {
//        return session.getUri().getQuery().split("=")[1];
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
//        sessions.values().remove(session);
//    }
//}