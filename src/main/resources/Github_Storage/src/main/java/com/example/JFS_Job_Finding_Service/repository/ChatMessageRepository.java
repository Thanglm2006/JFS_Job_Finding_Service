package com.example.JFS_Job_Finding_Service.repository;
import com.example.JFS_Job_Finding_Service.models.ChatMessage;
import com.example.JFS_Job_Finding_Service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    ChatMessage findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT DISTINCT m.receiver FROM ChatMessage m WHERE m.sender = :sender or m.receiver = :sender")
    List<User> findReceiversBySender(@Param("sender") User sender);
    @Query("select distinct m.sender from ChatMessage m where m.receiver = :receiver or m.sender = :receiver")
    List<User> findSendersByReceiver(@Param("receiver") User receiver);
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.sender = :sender AND m.receiver = :receiver " +
            "ORDER BY m.timestamp DESC")
    List<ChatMessage> findMessagesFromSenderToReceiver(
            @Param("sender") User sender,
            @Param("receiver") User receiver,
            Pageable pageable
    );
}
