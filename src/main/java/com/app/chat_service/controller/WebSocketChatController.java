package com.app.chat_service.controller;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ClearChatRequest;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.dto.TypingStatusDTO;
import com.app.chat_service.kakfa.KafkaMessageProcessorService;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.ClearedChatRepository;
import com.app.chat_service.service.ChatForwardService;
import com.app.chat_service.service.ChatMessageOverviewService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.ClearedChatService;
import com.app.chat_service.service.TeamService;
import com.app.chat_service.service.UpdateChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class WebSocketChatController {

   
    private final ChatPresenceTracker chatTracker;
    private final ChatMessageService chatMessageService;
    private final ChatForwardService chatForwardService;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UpdateChatMessageService updateChatMessageService;
    private final ClearedChatRepository clearedChatRepository;
    private final ClearedChatService clearedChatService;
    private final KafkaMessageProcessorService messageProcessor;
    private final ChatMessageOverviewService chatMessageOverviewService;
    private final TeamService teamService;
    
    public WebSocketChatController(
                                   ChatPresenceTracker chatTracker,
                                   ChatMessageService chatMessageService,
                                   ChatForwardService chatForwardService,
                                   ChatMessageRepository chatMessageRepository,
                                   SimpMessagingTemplate messagingTemplate,
                                   UpdateChatMessageService updateChatMessageService,
                                   ClearedChatRepository clearedChatRepository,
                                   ClearedChatService clearedChatService,
                                   KafkaMessageProcessorService messageProcessor,
                                   ChatMessageOverviewService chatMessageOverviewService,
                                   TeamService teamService) {
        this.chatTracker = chatTracker;
        this.chatMessageService = chatMessageService;
        this.chatForwardService = chatForwardService;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.updateChatMessageService = updateChatMessageService;
        this.clearedChatRepository = clearedChatRepository;
        this.clearedChatService = clearedChatService;
        this.messageProcessor=messageProcessor;
        this.chatMessageOverviewService=chatMessageOverviewService;
        this.teamService=teamService;
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
    @MessageMapping("/send")
    @Transactional
    public void handleMessage(@Payload ChatMessageRequest request) {
    	log.info("app/chat/send  METHOD CALLED");
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
        

//      CACHE CLEAR LOGIC FOR PRIVATE AND GROUPS WHEN NEW MESSAGE IS ARRIVED
//        
//        if("PRIVATE".equalsIgnoreCase(request.getType())) {
//        	chatMessageOverviewService.evictFirstPageCache(request.getSender(), request.getReceiver());
//        	chatMessageOverviewService.evictFirstPageCache(request.getReceiver(), request.getSender());    
//        	
//        	log.info("calling the evicted method for private type");
//        	}
//        
//        else if ("TEAM".equalsIgnoreCase(request.getType())) {
//        	List<String> memberIds= teamService.getEmployeeIdsByTeamId(request.getGroupId());
//        	
//        	if(memberIds!=null) {
//        		for(String memberId : memberIds) {
//        			chatMessageOverviewService.evictFirstPageCache(memberId, request.getGroupId());
//        			}
//        	}
//		}

        if (savedMessage.getClientId() != null) {
            String ackQueue = "PRIVATE".equalsIgnoreCase(savedMessage.getType())
                    ? "/queue/private-ack"
                    : "/queue/group-ack";

            messagingTemplate.convertAndSendToUser(
                    savedMessage.getSender(),
                    ackQueue,
                    savedMessage
            );

            log.info("✅ Sent ACK for message ID {} to sender {} on queue {}",
                    savedMessage.getId(), savedMessage.getSender(), ackQueue);
        }

        // chatKafkaProducer.send(savedMessage);
        
        messageProcessor.processChatMessage(savedMessage);
        chatMessageService.broadcastOverviewAsynchronously(
                request.getSender(),
                request.getReceiver(),
                request.getGroupId(),
                request.getType()
        );
    }

    // Edit existing message
    @MessageMapping("/edit")
    public void handleEditMessage(@Payload ChatMessageRequest updatedRequest) {
        log.info("✏️ Edit request received: {}", updatedRequest);

        if (updatedRequest.getMessageId() == null) {
            log.warn("❌ Edit request missing messageId");
            return;
        }

        String result = updateChatMessageService.updateChatMessage(
                updatedRequest.getMessageId(),
                updatedRequest
        );

        if (result.startsWith("Error")) {
            log.warn("❌ Edit failed: {}", result);
        } else {
            log.info("✅ Message edited successfully with id {} and content {}",
                    updatedRequest.getMessageId(), updatedRequest.getContent());
        }
    }

    // Reply to message
    @MessageMapping("/reply")
    public void handleReply(ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
    }

    // Forward message
    @MessageMapping("/forward")
    public void handleForward(ReplyForwardMessageDTO dto) {
        chatForwardService.handleReplyOrForward(dto);
    }

    /**
     * Endpoint for clearing a chat
     * @param dto - contains userId and chatId
     */
    @MessageMapping("/clear")
    public void clearChat(@Payload ClearChatRequest dto, Principal principal) {
        String authenticatedUserId = principal.getName();
        clearedChatService.clearChat(authenticatedUserId, dto.getChatId());

        messagingTemplate.convertAndSendToUser(
                authenticatedUserId,
                "/queue/clearchat",
                Map.of(
                        "message", "Chat cleared successfully for chatId: " + dto.getChatId(),
                        "chatId", dto.getChatId()
                )
        );
    }      
    
    /**
     * Handles typing status updates from clients.
     */
    @MessageMapping("/typing")
    public void handleTypingStatus(@Payload TypingStatusDTO dto) {
        log.info("Typing status received: {}", dto);
 
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            // Private chat: send to the specific receiver's queue
            messagingTemplate.convertAndSendToUser(dto.getReceiverId(),"/queue/typing-status",dto );
                    
        } else if ("TEAM".equalsIgnoreCase(dto.getType()) || "DEPARTMENT".equalsIgnoreCase(dto.getType())) {
        	// Group chat: broadcast to the group's topic
        	messagingTemplate.convertAndSend("/topic/typing-status/" + dto.getGroupId(),dto);
        }
    }

}
