package com.app.chat_service.service;

import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;

import jakarta.transaction.Transactional;

@Service
public class UpdateChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public UpdateChatMessageService(ChatMessageRepository chatMessageRepository,
                                    SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Updates the content of an existing chat message.
     * Only the original sender is allowed to edit the message.
     * Only the 'content' field is updated.
     * 
     * @param messageId The ID of the message to update.
     * @param updatedRequest The request object containing the sender and new content.
     * @return A success or error message string.
     */
    @Transactional
    public String updateChatMessage(Long messageId, ChatMessageRequest updatedRequest) {
        // 1. Find the message by its ID
        Optional<ChatMessage> optionalMessage = chatMessageRepository.findById(messageId);
        if (optionalMessage.isEmpty()) {
            return "Error: Message not found with ID " + messageId;
        }

        ChatMessage message = optionalMessage.get();

        // 2. Validate sender info is present
        if (updatedRequest == null || !StringUtils.hasText(updatedRequest.getSender())) {
            return "Error: Sender information is required for validation.";
        }

        // 3. Check if the sender is the original sender of the message
        if (!updatedRequest.getSender().equals(message.getSender())) {
            return "Error: You are not authorized to edit this message.";
        }

        // 4. Validate and update content
        if (StringUtils.hasText(updatedRequest.getContent())) {
            message.setContent(updatedRequest.getContent());
        } else {
            return "Error: Updated content cannot be null or empty.";
        }

        // 5. Save updated message
        chatMessageRepository.save(message);

        // 6. Prepare WebSocket response
        ChatMessageResponse response = new ChatMessageResponse(
                message.getId(),
                message.getSender(),
                message.getReceiver(),
                message.getGroupId(),
                message.getContent(),
                message.getFileName(),
                message.getFileType(),
                message.getFileSize(),
                message.getType(),
                message.getTimestamp(),
                message.getFileData(),
                message.getClientId()
        );

        // 7. Send WebSocket update
        String type = message.getType();
        if (type != null) {
            switch (type) {
                case "PRIVATE" -> messagingTemplate.convertAndSend("/topic/private/" + message.getReceiver(), response);
                case "TEAM" -> messagingTemplate.convertAndSend("/topic/team/" + message.getGroupId(), response);
                case "DEPARTMENT" -> messagingTemplate.convertAndSend("/topic/department/" + message.getGroupId(), response);
                default -> System.out.println("Unknown message type for WebSocket broadcast: " + type);
            }
        }

        return "Message updated successfully";
    }
}
