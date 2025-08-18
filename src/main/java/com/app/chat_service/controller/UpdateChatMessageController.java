package com.app.chat_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.service.UpdateChatMessageService;

@RestController
@RequestMapping("/api")
public class UpdateChatMessageController {

    private final UpdateChatMessageService updateChatMessageService;

    public UpdateChatMessageController(UpdateChatMessageService updateChatMessageService) {
        this.updateChatMessageService = updateChatMessageService;
    }

    /**
     * Updates an existing chat message's content.
     * Only the original sender is allowed to edit.
     *
     * @param messageId The ID of the message to update.
     * @param updatedRequest The request body containing sender and new content.
     * @return ResponseEntity with success or error message.
     */
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
