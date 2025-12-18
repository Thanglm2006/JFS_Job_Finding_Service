package com.example.JFS_Job_Finding_Service.repository;
import com.example.JFS_Job_Finding_Service.DTO.Chat.userInChat;
import com.example.JFS_Job_Finding_Service.models.ChatMessage;
import com.example.JFS_Job_Finding_Service.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE (m.sender.id = :user1Id AND m.receiver.id = :user2Id) " +
            "OR (m.sender.id = :user2Id AND m.receiver.id = :user1Id) " +
            "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findConversation(
                                        @Param("user1Id") Long user1Id,
                                        @Param("user2Id") Long user2Id,
                                        Pageable pageable
    );

    // Logic: Tìm tin nhắn mới nhất (Max ID) cho mỗi cặp hội thoại liên quan đến :myId
    @Query(value = """
        SELECT 
            u.id AS userId,
            u.full_name AS fullName,
            u.avatar_url AS avatarUrl,
            m.message AS lastMessage,
            m.file_url AS fileUrl,
            m.timestamp AS lastMessageTime,
            m.sender_id AS lastSenderId
        FROM users u
        INNER JOIN (
            SELECT 
                CASE 
                    WHEN sender_id = :myId THEN receiver_id 
                    ELSE sender_id 
                END AS partner_id,
                MAX(id) as max_msg_id
            FROM chat_messages 
            WHERE sender_id = :myId OR receiver_id = :myId
            GROUP BY partner_id
        ) latest_msg ON u.id = latest_msg.partner_id
        INNER JOIN chat_messages m ON m.id = latest_msg.max_msg_id
        ORDER BY m.timestamp DESC
    """, nativeQuery = true)
    List<InboxProjection> findInboxList(@Param("myId") Long myId);

    interface InboxProjection {
        Long getUserId();
        String getFullName();
        String getAvatarUrl();
        String getLastMessage();
        String getFileUrl();
        java.sql.Timestamp getLastMessageTime();
        Long getLastSenderId();
    }
}
