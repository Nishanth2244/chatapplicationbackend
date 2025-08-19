package com.app.chat_service.controller;
 
import com.app.chat_service.dto.PinnedMessageDTO;
import com.app.chat_service.service.MessagePinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.Optional;
 
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class MessagePinController {
 
    private final MessagePinService messagePinService;
 
    @PostMapping("/pin/{messageId}")
    public ResponseEntity<PinnedMessageDTO> pinMessage(@PathVariable Long messageId, @RequestParam String userId) {
        PinnedMessageDTO pinnedMessage = messagePinService.pinMessage(messageId, userId);
        return ResponseEntity.ok(pinnedMessage);
    }
 
    @PostMapping("/unpin/{messageId}")
    public ResponseEntity<Void> unpinMessage(@PathVariable Long messageId, @RequestParam String userId) {
        messagePinService.unpinMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }
 
    @GetMapping("/{chatId}/pinned")
    public ResponseEntity<PinnedMessageDTO> getPinnedMessage(
            @PathVariable String chatId,
            @RequestParam String chatType,
            @RequestParam String userId) {
        Optional<PinnedMessageDTO> pinnedMessage;
        if ("group".equalsIgnoreCase(chatType)) {
            pinnedMessage = messagePinService.getPinnedMessageForChat(chatId);
        } else {
            pinnedMessage = messagePinService.getPinnedMessageForPrivateChat(userId, chatId);
        }
        return pinnedMessage.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}        