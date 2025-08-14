package com.app.chat_service.controller;
 
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.service.ChatForwardService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate; // <<< 1. IMPORT SIMPMESSAGINGTEMPLATE
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
    private final SimpMessagingTemplate messagingTemplate; // <<< 2. INJECT SIMPMESSAGINGTEMPLATE
 
    public WebSocketChatController(ChatKafkaProducer chatKafkaProducer,
                                   ChatPresenceTracker chatTracker,
                                   ChatMessageService chatMessageService,
                                   ChatForwardService chatForwardService,
                                   ChatMessageRepository chatMessageRepository,
                                   SimpMessagingTemplate messagingTemplate) { // <<< 3. ADD TO CONSTRUCTOR
        this.chatKafkaProducer = chatKafkaProducer;
        this.chatTracker = chatTracker;
        this.chatMessageService = chatMessageService;
        this.chatForwardService = chatForwardService;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate; // <<< 4. INITIALIZE IT
    }
 
    /** Handles direct or group chat messages (This seems to be an older mapping, keeping as is) */
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
 
        // 1. Message ni Database lo save cheyandi.
        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("✅ Message saved to DB with ID: {}", savedMessage.getId());
 
        // ======================= BUG FIX START =======================
        // 2. Sender ki "nee message save ayyindi" ani acknowledgement (ACK) pampandi.
        //    Ide "Sending..." status ni fix chestundi.
        if (savedMessage.getClientId() != null) {
            String ackQueue;
            // Decide which ACK queue to use based on chat type
            if ("PRIVATE".equalsIgnoreCase(savedMessage.getType())) {
                ackQueue = "/queue/private-ack";
            } else {
                ackQueue = "/queue/group-ack";
            }
            // Send the saved message (with DB ID) back to the sender's private ACK queue
            messagingTemplate.convertAndSendToUser(savedMessage.getSender(), ackQueue, savedMessage);
            log.info("✅ Sent ACK for message ID {} to sender {} on queue {}", savedMessage.getId(), savedMessage.getSender(), ackQueue);
        }
        // ======================= BUG FIX END =========================
 
        // 3. Save chesina message (with DB ID) ni Kafka ki pampandi (Receiver kosam).
        //    Ee existing logic ni manam change cheyatledu.
        chatKafkaProducer.send(savedMessage);
 
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
        chatForwardService.handleReplyOrForward(dto);
    }
 
    @MessageMapping("/chat/forward")
    public void handleForward(ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
    }
}