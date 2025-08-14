package com.app.chat_service.controller;
 
import com.app.chat_service.service.MessageDeleteService;
import com.app.chat_service.service.UpdateChatMessageService;
import com.app.chat_service.dto.ChatMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
@RequestMapping("/api/chat") // Consolidated base path
@RestController
public class DeleteMessageController {
 
    private final MessageDeleteService messageDeleteService;
    private final UpdateChatMessageService updateChatMessageService;
 
    @Autowired
    public DeleteMessageController(MessageDeleteService messageDeleteService, UpdateChatMessageService updateChatMessageService) {
        this.messageDeleteService = messageDeleteService;
        this.updateChatMessageService = updateChatMessageService;
    }
 
    // Soft delete for current user (hide message for that user only)
    @PostMapping("/{messageId}/me")
    public ResponseEntity<?> deleteForMe(
            @PathVariable Long messageId,
            @RequestParam String userId) {
        try {
            messageDeleteService.deleteForMe(messageId, userId);
            return ResponseEntity.ok("Message hidden for user: " + userId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to hide message: " + e.getMessage());
        }
    }
 
    // Soft delete for everyone (only sender can do this)
    @PostMapping("/{messageId}/everyone")
    public ResponseEntity<?> deleteForEveryone(
            @PathVariable Long messageId,
            @RequestParam String userId) {
        try {
            messageDeleteService.deleteForEveryone(messageId, userId);
            return ResponseEntity.ok("Message deleted for everyone");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete message for everyone: " + e.getMessage());
        }
    }
   
    @PutMapping("/update/{messageId}")
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