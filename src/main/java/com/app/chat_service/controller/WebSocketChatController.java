package com.app.chat_service.controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.app.chat_service.dto.ChatMessageRequest;

@Controller
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/send") // maps to /app/chat/send
    public void handleMessage(@Payload ChatMessageRequest message) {
    	
    	
        // Send to appropriate destination
    		String destination = switch (message.getType().toUpperCase()) {
            case "PRIVATE" -> "/queue/private-" + message.getReceiver();
            case "TEAM" -> "/topic/team-" + message.getGroupId();
            case "DEPARTMENT" -> "/topic/department-" + message.getGroupId();
            default -> throw new IllegalArgumentException("Unknown message type");
        };
        System.out.println("jfnowrfroewo");

        messagingTemplate.convertAndSend(destination, message);
    }
}