package com.example.JFS_Job_Finding_Service.ultils;

import com.example.JFS_Job_Finding_Service.Services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ChatService chatService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String id = getId(session);
        sessions.put(id, session);
        System.out.println(id + " connected");
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Format: recipient:message
        String[] parts = message.getPayload().split(":", 2);
        String recipient = parts[0];
        String content = parts[1];
        chatService.saveMessage(Long.parseLong(getId(session)), Long.parseLong(recipient), content);
        WebSocketSession recipientSession = sessions.get(recipient);
        if (recipientSession != null && recipientSession.isOpen()) {
            System.out.println("Sending message to " + recipient+ ": " + content);
            recipientSession.sendMessage(new TextMessage(getId(session) + ": " + content));
        }
    }

    private String getId(WebSocketSession session) {
        return session.getUri().getQuery().split("=")[1];
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().remove(session);
    }
}
