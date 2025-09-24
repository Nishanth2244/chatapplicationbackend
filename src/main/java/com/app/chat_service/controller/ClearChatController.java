package com.app.chat_service.controller;

import com.app.chat_service.service.ClearedChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/chat")
@RequiredArgsConstructor
public class ClearChatController {

    private final ClearedChatService clearedChatService;

    @PostMapping("/clear")
    public ResponseEntity<String> clearChat(
            @RequestParam String userId,
            @RequestParam String chatId) {

        clearedChatService.clearChat(userId, chatId);
        return ResponseEntity.ok("Chat cleared for user " + userId);
    }
}
