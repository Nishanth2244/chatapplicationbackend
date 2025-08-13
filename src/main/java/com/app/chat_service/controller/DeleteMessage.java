package com.app.chat_service.controller;

import com.app.chat_service.service.MessageDeleteService;
import com.app.chat_service.service.UpdateChatMessageService;
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.repo.ChatMessageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api")
@RestController
public class DeleteMessage {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageDeleteService messageDeleteService;
    
    @Autowired
    UpdateChatMessageService updateChatMessageService;

    @Autowired
    public DeleteMessage(ChatMessageRepository chatMessageRepository,
                         SimpMessagingTemplate messagingTemplate,
                         MessageDeleteService messageDeleteService) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.messageDeleteService = messageDeleteService;
    }

    // 1️⃣ Hard delete from DB (not recommended if you want soft delete behavior)
    @DeleteMapping("/chat/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        try {
            chatMessageRepository.deleteById(messageId);

            // Notify frontend via WebSocket (all clients)
            messagingTemplate.convertAndSend("/topic/message-deleted", messageId);

            return ResponseEntity.ok("Message deleted and notified");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete message: " + e.getMessage());
        }
    }

    // 2️⃣ Soft delete for current user (hide message for that user only)
    @PostMapping("/chat/{messageId}/me")
    public ResponseEntity<?> deleteForMe(
            @PathVariable Long messageId,
            @RequestParam String userId) {
        try {
            messageDeleteService.deleteForMe(messageId, userId);

            // Notify only this user to update UI
            messagingTemplate.convertAndSendToUser(userId, "/queue/message-deleted", messageId);

            return ResponseEntity.ok("Message hidden for user: " + userId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to hide message: " + e.getMessage());
        }
    }

    // 3️⃣ Soft delete for everyone (only sender can do this)
    @PostMapping("/chat/{messageId}/everyone")
    public ResponseEntity<?> deleteForEveryone(
            @PathVariable Long messageId,
            @RequestParam String userId) {
        try {
            messageDeleteService.deleteForEveryone(messageId, userId);

            // Notify all clients that message is deleted for everyone
            messagingTemplate.convertAndSend("/topic/message-deleted", messageId);

            return ResponseEntity.ok("Message deleted for everyone");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete message for everyone: " + e.getMessage());
        }
    }
    
    @PutMapping("/chat/update/{messageId}")
    public ResponseEntity<String> updateMessage(
            @PathVariable Long messageId,
            @RequestBody ChatMessageRequest updatedRequest) {

        String result = updateChatMessageService.updateChatMessage(messageId, updatedRequest);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

}
