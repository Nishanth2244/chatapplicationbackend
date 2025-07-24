package com.app.chat_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.app.chat_service.repo.ChatMessageRepository;

@RequestMapping("/api")
@RestController
public class DeleteMessage {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

//    //✅ WebSocket Frontend
//    On the frontend, subscribe to /topic/message-deleted:
//
//    	javascript
//    	Copy
//    	Edit
//    	stompClient.subscribe("/topic/message-deleted", (msg) => {
//    	  const deletedMessageId = JSON.parse(msg.body);
//    	  removeMessageFromUI(deletedMessageId);
//    	});
//    

    
    @DeleteMapping("/chat/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        try {
            chatMessageRepository.deleteById(messageId);

            // Notify frontend via WebSocket
            messagingTemplate.convertAndSend("/topic/message-deleted", messageId);

            return ResponseEntity.ok("Message deleted and notified");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete message");
        }
    }
}