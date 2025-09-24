package com.app.chat_service.service;
 
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
 
import java.util.Optional;
 
@Service
public class UpdateChatMessageService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
 
    public UpdateChatMessageService(ChatMessageRepository chatMessageRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }
 
    @Transactional
    public String updateChatMessage(Long messageId, ChatMessageRequest updatedRequest) {
        Optional<ChatMessage> optionalMessage = chatMessageRepository.findById(messageId);
        if (optionalMessage.isEmpty()) {
            return "Error: Message not found with ID " + messageId;
        }
 
        ChatMessage message = optionalMessage.get();
 
        if (updatedRequest == null || !StringUtils.hasText(updatedRequest.getSender())) {
            return "Error: Sender information is required for validation.";
        }
 
        if (!updatedRequest.getSender().equals(message.getSender())) {
            return "Error: You are not authorized to edit this message.";
        }
        
        if (StringUtils.hasText(updatedRequest.getContent())) {
            message.setContent(updatedRequest.getContent());
            message.setEdited(true); 
        } else {
            return "Error: Updated content cannot be null or empty.";
        }
        ChatMessage savedMessage = chatMessageRepository.save(message);
 
        // Prepare the response DTO with the isEdited flag
        ChatMessageResponse response = new ChatMessageResponse(
                savedMessage.getId(),
                savedMessage.getSender(),
                savedMessage.getReceiver(),
                savedMessage.getGroupId(),
                savedMessage.getContent(),
                savedMessage.getFileName(),
                savedMessage.getFileType(),
                savedMessage.getFileSize(),
                savedMessage.getType(),
                savedMessage.getTimestamp(),
                null, 
                savedMessage.getClientId(),
                									
                savedMessage.isEdited(),		
                savedMessage.getDuration()
        
        	);
        
 
        String type = savedMessage.getType();
        if (type != null) {
            String destination;
            if ("PRIVATE".equals(type)) {
                if (savedMessage.getReceiver() != null) {
                    destination = "/user/" + savedMessage.getReceiver() + "/queue/private";
                    messagingTemplate.convertAndSend(destination, response);
                }
                // Also send to sender's queue to update their other sessions
                if(savedMessage.getSender() != null) {
                    destination = "/user/" + savedMessage.getSender() + "/queue/private";
                    messagingTemplate.convertAndSend(destination, response);
                }
 
            } else if ("TEAM".equals(type) || "DEPARTMENT".equals(type)) {
                if (savedMessage.getGroupId() != null) {
                    destination = "/topic/team-" + savedMessage.getGroupId();
                    messagingTemplate.convertAndSend(destination, response);
                }
            } else {
                System.out.println("Warning: Unknown message type for WebSocket broadcast: " + type);
            }
        }
 
        return "Message updated successfully";
    }
}