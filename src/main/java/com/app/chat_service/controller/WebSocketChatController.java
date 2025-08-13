package com.app.chat_service.controller;
 
import java.security.Principal;
import java.time.LocalDateTime;
 
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
 
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository; // <<< 1. REPOSITORY IMPORT CHEYANDI
import com.app.chat_service.service.ChatForwardService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
 
import lombok.extern.slf4j.Slf4j;
 
@Controller
@Slf4j
public class WebSocketChatController {
 
    private final ChatKafkaProducer chatKafkaProducer;
    private final ChatPresenceTracker chatTracker;
    private final ChatMessageService chatMessageService;
    private final ChatForwardService chatForwardService;
    private final ChatMessageRepository chatMessageRepository; // <<< 2. REPOSITORY INJECT CHEYANDI
 
    public WebSocketChatController(ChatKafkaProducer chatKafkaProducer,
                                     ChatPresenceTracker chatTracker,
                                     ChatMessageService chatMessageService,
                                     ChatForwardService chatForwardService,
                                     ChatMessageRepository chatMessageRepository) { // <<< 3. CONSTRUCTOR LO ADD CHEYANDI
        this.chatKafkaProducer = chatKafkaProducer;
        this.chatTracker = chatTracker;
        this.chatMessageService = chatMessageService;
        this.chatForwardService = chatForwardService;
        this.chatMessageRepository = chatMessageRepository; // <<< 4. INITIALIZE CHEYANDI
    }
 
    /** Handles direct or group chat messages */
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
       
        // BUG FIX: Message ni save cheyandi
        ChatMessage savedMessage = chatMessageRepository.save(message);
       
        chatKafkaProducer.send(savedMessage);
 
        // Sidebar update for sender
        chatMessageService.broadcastChatOverview(sender);
 
        // Update receiver sidebar only for private chat
        if (!message.isGroup()) {
            chatMessageService.broadcastChatOverview(target);
        } else {
            chatMessageService.broadcastGroupChatOverview(message.getGroupId());
        }
    }
 
    /** Triggered when user opens a chat window (presence) */
    @MessageMapping("/presence/open/{target}")
    public void openChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        chatTracker.openChat(user, target);
 
        if (target.toUpperCase().startsWith("TEAM")) {
            chatMessageService.markGroupMessagesAsRead(user, target);
        } else {
            chatMessageService.markMessagesAsRead(user, target);
        }
 
        chatMessageService.broadcastChatOverview(user);
        chatMessageService.broadcastChatOverview(target);
    }
 
    /** Triggered when user closes a chat window (presence) */
    @MessageMapping("/presence/close/{target}")
    public void closeChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        chatTracker.closeChat(user, target);
 
        log.info("❌ Chat closed from {} to {}", user, target);
        chatMessageService.broadcastChatOverview(user);
        chatMessageService.broadcastChatOverview(target);
    }
 
    /** Handles messages from custom chat UI (no {target} mapping) */
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
 
        // ======================= BUG FIX START =======================
        // 1. Message ni Database lo save cheyandi.
        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("✅ Message saved to DB with ID: {}", savedMessage.getId());
 
        // 2. Save chesina message (with DB ID) ni Kafka ki pampandi.
        chatKafkaProducer.send(savedMessage);
        // ======================= BUG FIX END =========================
 
        // Update sender sidebar
        chatMessageService.broadcastChatOverview(request.getSender());
 
        // For private chats, update receiver sidebar. For groups, update all members.
        if ("PRIVATE".equalsIgnoreCase(request.getType()) && request.getReceiver() != null) {
            chatMessageService.broadcastChatOverview(request.getReceiver());
        } else if ("TEAM".equalsIgnoreCase(request.getType()) && request.getGroupId() != null) {
            chatMessageService.broadcastGroupChatOverview(request.getGroupId());
        }
    }
 
    @MessageMapping("/chat/reply")
    public void handleReply(ReplyForwardMessageDTO dto) {
        // NOTE: Ee method lo kuda message save avvali.
        // chatForwardService lo aa logic undemo check cheyandi.
        chatForwardService.handleReplyOrForward(dto);
    }
 
    @MessageMapping("/chat/forward")
    public void handleForward(ReplyForwardMessageDTO dto) {
        // NOTE: Ee method lo kuda message save avvali.
        // chatForwardService lo aa logic undemo check cheyandi.
        chatForwardService.handleReplyOrForward(dto);
    }
}
 
 