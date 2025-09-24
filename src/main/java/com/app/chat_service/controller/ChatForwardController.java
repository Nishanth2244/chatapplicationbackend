package com.app.chat_service.controller;

import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.service.ChatForwardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatForwardController {

    @Autowired
    ChatForwardService chatForwardService;

    // ======================= BUG FIX START =======================
    // Changed response type to return a generic success message.
    @PostMapping("/reply")
    public ResponseEntity<?> handleReply(@RequestBody ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Reply sent successfully."));
    }

    @PostMapping("/forward")
    public ResponseEntity<?> handleForward(@RequestBody ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Message forwarded successfully."));
    }
    // ======================= BUG FIX END =======================
}
