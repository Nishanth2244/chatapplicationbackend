package com.app.chat_service.controller;

import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.service.ChatForwardService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatForwardController {

    @Autowired
    ChatForwardService chatForwardService;

    /**
     * REST endpoint to handle reply.
     * Uses the same service logic as WebSocket.
     */
    @PostMapping("/reply")
    public ResponseEntity<ChatMessageResponse> handleReply(@RequestBody ReplyForwardMessageDTO dto) {
        ChatMessageResponse response = chatForwardService.handleReplyOrForward(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * REST endpoint to handle message forwarding.
     */
    @PostMapping("/forward")
    public ResponseEntity<ChatMessageResponse> handleForward(@RequestBody ReplyForwardMessageDTO dto) {
        ChatMessageResponse response = chatForwardService.handleReplyOrForward(dto);
        return ResponseEntity.ok(response);
    }
}
