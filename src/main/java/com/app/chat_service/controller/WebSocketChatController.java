package com.app.chat_service.controller;
 
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.service.ChatForwardService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.UpdateChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
 
import java.security.Principal;
import java.time.LocalDateTime;
 
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
 
    public WebSocketChatController(ChatKafkaProducer chatKafkaProducer,
                                     ChatPresenceTracker chatTracker,
                                     ChatMessageService chatMessageService,
                                     ChatForwardService chatForwardService,
                                     ChatMessageRepository chatMessageRepository,
                                     SimpMessagingTemplate messagingTemplate,
                                     UpdateChatMessageService updateChatMessageService) {
        this.chatKafkaProducer = chatKafkaProducer;
        this.chatTracker = chatTracker;
        this.chatMessageService = chatMessageService;
        this.chatForwardService = chatForwardService;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.updateChatMessageService = updateChatMessageService;
    }
 
    // No changes to this method
    @MessageMapping("/chat/{target}")
    public void sendMessage(@DestinationVariable String target,
                            @Payload ChatMessage message,
                            Principal principal) {
        String sender = principal.getName();
        message.setSender(sender);
        message.setReceiver(target);
        message.setType(message.isGroup() ? "TEAM" : "PRIVATE");
        message.setTimestamp(LocalDateTime.now());
 
        log.info("📩 Chat message from {} to {}", sender, target);
        ChatMessage savedMessage = chatMessageRepository.save(message);
        chatKafkaProducer.send(savedMessage);
 
        chatMessageService.broadcastChatOverview(sender);
 
        if (!message.isGroup()) {
            chatMessageService.broadcastChatOverview(target);
        } else {
            chatMessageService.broadcastGroupChatOverview(message.getGroupId());
        }
    }
 
    // =================================================================================
    // ✅ FINAL UNREAD COUNT BUG FIX STARTS HERE
    // =================================================================================
    @MessageMapping("/presence/open/{target}")
    public void openChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        chatTracker.openChat(user, target);
 
        // This part is correct. It starts the process of marking messages as read.
        if (target.toUpperCase().startsWith("TEAM")) {
            chatMessageService.markGroupMessagesAsRead(user, target);
        } else {
            chatMessageService.markMessagesAsRead(user, target);
        }
 
        // ✅ FIX: REMOVED the broadcast calls from here.
        // The broadcast should ONLY happen from within the service layer, AFTER the
        // database transaction is successfully committed. Calling it here causes a
        // race condition where the old unread count is sent before the update is finished.
        // chatMessageService.broadcastChatOverview(user);       // <--- REMOVED
        // chatMessageService.broadcastChatOverview(target);     // <--- REMOVED
    }
    // =================================================================================
    // ✅ FINAL FIX ENDS HERE
    // =================================================================================
 
    // No changes to this method
    @MessageMapping("/presence/close/{target}")
    public void closeChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        chatTracker.closeChat(user, target);
 
        log.info("❌ Chat closed from {} to {}", user, target);
        chatMessageService.broadcastChatOverview(user);
        chatMessageService.broadcastChatOverview(target);
    }
 
    // No changes to this method
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
            String ackQueue;
            if ("PRIVATE".equalsIgnoreCase(savedMessage.getType())) {
                ackQueue = "/queue/private-ack";
            } else {
                ackQueue = "/queue/group-ack";
            }
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
 
    // No changes to other methods...
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
            log.info("✅ Message edited successfully with id {} and content {}", updatedRequest.getMessageId(),updatedRequest.getContent());
        }
    }
   
    @MessageMapping("/chat/reply")
    public void handleReply(ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
    }
 
    @MessageMapping("/chat/forward")
    public void handleForward(ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
    }
}