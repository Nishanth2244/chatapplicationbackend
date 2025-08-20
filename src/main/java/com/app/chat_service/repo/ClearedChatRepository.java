package com.app.chat_service.repo;

import com.app.chat_service.model.ClearedChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClearedChatRepository extends JpaRepository<ClearedChat, Long> {

    Optional<ClearedChat> findByUserIdAndChatId(String userId, String chatId);
}
