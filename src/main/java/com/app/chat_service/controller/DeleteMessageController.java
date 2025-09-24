package com.app.chat_service.controller;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.chat_service.service.MessageDeleteService;
 
@RequestMapping("/api/chat") // Consolidated base path
@RestController
public class DeleteMessageController {
 
    private final MessageDeleteService messageDeleteService;
 
    @Autowired
    public DeleteMessageController(MessageDeleteService messageDeleteService) {
        this.messageDeleteService = messageDeleteService;
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
    
    
    
   
}