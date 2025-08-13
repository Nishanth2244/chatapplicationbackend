package com.app.chat_service.repo;

import com.app.chat_service.model.MessageAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageActionRepository extends JpaRepository<MessageAction, Long> {

    @Query("""
        SELECT m FROM MessageAction m
        WHERE m.messageId IN :messageIds
        AND (m.userId = :userId OR m.actionType = 'DELETE_ALL')
    """)
    List<MessageAction> findDeleteActionsForUser(
            @Param("messageIds") List<Long> messageIds,
            @Param("userId") String userId);
}
