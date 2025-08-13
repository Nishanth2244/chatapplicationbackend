package com.app.chat_service.service;
 
import java.util.Optional;
 
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Import StringUtils
 
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
     * Only the 'content' field is updated to prevent nulling out other important fields.
     * @param messageId The ID of the message to update.
     * @param updatedRequest The request object containing the new content.
     * @return A success or error message string.
     */
    @Transactional
    public String updateChatMessage(Long messageId, ChatMessageRequest updatedRequest) {
        // 1. Find the message by its ID
        Optional<ChatMessage> optionalMessage = chatMessageRepository.findById(messageId);
        if (optionalMessage.isEmpty()) {
            // Return an error if the message doesn't exist
            return "Error: Message not found with ID " + messageId;
        }
 
        ChatMessage message = optionalMessage.get();
 
        // 2. --- FIX APPLIED HERE ---
        // We only update the content of the message.
        // We check if the new content is actually provided in the request.
        if (updatedRequest != null && StringUtils.hasText(updatedRequest.getContent())) {
            message.setContent(updatedRequest.getContent());
        } else {
            // If the content in the request is null or empty, we don't update.
            return "Error: Updated content cannot be null or empty.";
        }
        // We DO NOT update sender, receiver, type, or groupId during an edit.
 
        // 3. Save the updated message back to the database
        chatMessageRepository.save(message);
 
        // 4. Prepare the response to be sent over WebSocket
        ChatMessageResponse response = new ChatMessageResponse(
                message.getId(),
                message.getSender(),
                message.getReceiver(),
                message.getGroupId(),
                message.getContent(), // Send the new, updated content
                message.getFileName(),
                message.getFileType(),
                message.getFileSize(),
                message.getType(),
                message.getTimestamp(),
                message.getFileData(),
                message.getClientId()
                );
 
        // 5. Send the updated message to the relevant WebSocket topic
        String type = message.getType();
        if (type != null) {
            switch (type) {
                case "PRIVATE" -> {
                    String destination = "/topic/private/" + message.getReceiver();
                    messagingTemplate.convertAndSend(destination, response);
                }
                case "TEAM" -> {
                    String destination = "/topic/team/" + message.getGroupId();
                    messagingTemplate.convertAndSend(destination, response);
                }
                case "DEPARTMENT" -> {
                    String destination = "/topic/department/" + message.getGroupId();
                    messagingTemplate.convertAndSend(destination, response);
                }
                default -> System.out.println("Unknown message type for WebSocket broadcast: " + type);
            }
        }
 
        return "Message updated successfully";
    }
}
 