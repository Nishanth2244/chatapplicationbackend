package com.app.chat_service.controller;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ClearChatRequest;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.ClearedChatRepository;
import com.app.chat_service.service.ChatForwardService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.ClearedChatService;
import com.app.chat_service.service.UpdateChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@Slf4j
public class WebSocketChatController {

    private final ChatKafkaProducer chatKafkaProducer;
    private final ChatPresenceTracker chatTracker;
    private final ChatMessageService chatMessageService;
    private final ChatForwardService chatForwardService;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UpdateChatMessageService updateChatMessageService;
    private final ClearedChatRepository clearedChatRepository;
    private final ClearedChatService clearedChatService;

    public WebSocketChatController(ChatKafkaProducer chatKafkaProducer,
                                   ChatPresenceTracker chatTracker,
                                   ChatMessageService chatMessageService,
                                   ChatForwardService chatForwardService,
                                   ChatMessageRepository chatMessageRepository,
                                   SimpMessagingTemplate messagingTemplate,
                                   UpdateChatMessageService updateChatMessageService,
                                   ClearedChatRepository clearedChatRepository,
                                   ClearedChatService clearedChatService) {
        this.chatKafkaProducer = chatKafkaProducer;
        this.chatTracker = chatTracker;
        this.chatMessageService = chatMessageService;
        this.chatForwardService = chatForwardService;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.updateChatMessageService = updateChatMessageService;
        this.clearedChatRepository = clearedChatRepository;
        this.clearedChatService = clearedChatService;
    }

    // Mark chat as opened, start read process
    @MessageMapping("/presence/open/{target}")
    public void openChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        chatTracker.openChat(user, target);
        if (target.toUpperCase().startsWith("TEAM")) {
            chatMessageService.markGroupMessagesAsRead(user, target);
        } else {
            chatMessageService.markMessagesAsRead(user, target);
        }
        // Broadcasts are now inside service after DB commit (fixes unread count bug)
    }

    // Mark chat as closed
    @MessageMapping("/presence/close/{target}")
    public void closeChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        chatTracker.closeChat(user, target);
        log.info("❌ Chat closed from {} to {}", user, target);
        chatMessageService.broadcastChatOverview(user);
        chatMessageService.broadcastChatOverview(target);
    }

    // Send chat message (with ACK)
    @MessageMapping("/chat/send")
    public void handleMessage(@Payload ChatMessageRequest request) {
        log.info("➡️ Message received in backend: {}", request);
        ChatMessage message = new ChatMessage();
        message.setSender(request.getSender());
        message.setReceiver(request.getReceiver());
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setGroupId(request.getGroupId());
        message.setTimestamp(LocalDateTime.now());
        message.setClientId(request.getClientId());
        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("✅ Message saved to DB with ID: {}", savedMessage.getId());
        if (savedMessage.getClientId() != null) {
            String ackQueue = "PRIVATE".equalsIgnoreCase(savedMessage.getType())
                    ? "/queue/private-ack"
                    : "/queue/group-ack";
            messagingTemplate.convertAndSendToUser(savedMessage.getSender(), ackQueue, savedMessage);
            log.info("✅ Sent ACK for message ID {} to sender {} on queue {}", savedMessage.getId(), savedMessage.getSender(), ackQueue);
        }
        chatKafkaProducer.send(savedMessage);
        chatMessageService.broadcastChatOverview(request.getSender());
        if ("PRIVATE".equalsIgnoreCase(request.getType()) && request.getReceiver() != null) {
            chatMessageService.broadcastChatOverview(request.getReceiver());
        } else if ("TEAM".equalsIgnoreCase(request.getType()) && request.getGroupId() != null) {
            chatMessageService.broadcastGroupChatOverview(request.getGroupId());
        }
    }

    // Edit existing message
    @MessageMapping("/chat/edit")
    public void handleEditMessage(@Payload ChatMessageRequest updatedRequest) {
        log.info("✏️ Edit request received: {}", updatedRequest);
        if (updatedRequest.getMessageId() == null) {
            log.warn("❌ Edit request missing messageId");
            return;
        }
        String result = updateChatMessageService.updateChatMessage(updatedRequest.getMessageId(), updatedRequest);
        if (result.startsWith("Error")) {
            log.warn("❌ Edit failed: {}", result);
        } else {
            log.info("✅ Message edited successfully with id {} and content {}", updatedRequest.getMessageId(), updatedRequest.getContent());
        }
    }

    // Reply to message
    @MessageMapping("/chat/reply")
    public void handleReply(ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
    }

    // Forward message
    @MessageMapping("/chat/forward")
    public void handleForward(ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
    }

    /**
     * Endpoint for clearing a chat
     * @param dto - contains userId and chatId
     */
    @MessageMapping("/chat/clear")
    public void clearChat(@Payload ClearChatRequest dto, Principal principal) {
        // Instead of using userId from frontend, always take authenticated user ID.
        // This ensures only the logged-in user’s chat is cleared (for security).
        String authenticatedUserId = principal.getName();

        // Call service with authenticated user ID.
        clearedChatService.clearChat(authenticatedUserId, dto.getChatId());

        // Send notification only to the authenticated user.
        // For frontend readability, send a structured object instead of plain string.
        messagingTemplate.convertAndSendToUser(
                authenticatedUserId,
                "/queue/clearchat",
                Map.of(
                    "message", "Chat cleared successfully for chatId: " + dto.getChatId(),
                    "chatId", dto.getChatId()
                )
        );
    }
}
