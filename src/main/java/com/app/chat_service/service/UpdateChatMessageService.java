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
 
/**
 * Service responsible for handling updates to existing chat messages.
 */
@Service
public class UpdateChatMessageService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
 
    /**
     * Constructs the service with required dependencies.
     *
     * @param chatMessageRepository The repository for chat message data access.
     * @param messagingTemplate     The template for sending messages over WebSocket.
     */
    public UpdateChatMessageService(ChatMessageRepository chatMessageRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }
 
    /**
     * Updates the content of an existing chat message and broadcasts the change.
     * <p>
     * This method performs the following steps:
     * 1. Finds the message by its ID.
     * 2. Validates that the request comes from the original sender.
     * 3. Updates the message content in the database.
     * 4. Broadcasts the updated message to the relevant clients via WebSocket.
     * - For PRIVATE chats, it sends the update to both the sender's and receiver's private queues.
     * - For TEAM/DEPARTMENT chats, it sends the update to the shared group topic.
     *
     * @param messageId      The ID of the message to update.
     * @param updatedRequest The request object containing the new content and sender info.
     * @return A string indicating the result of the operation.
     */
    @Transactional
    public String updateChatMessage(Long messageId, ChatMessageRequest updatedRequest) {
        // 1. Find the message by its ID
        Optional<ChatMessage> optionalMessage = chatMessageRepository.findById(messageId);
        if (optionalMessage.isEmpty()) {
            return "Error: Message not found with ID " + messageId;
        }
 
        ChatMessage message = optionalMessage.get();
 
        // 2. Validate sender info is present in the request
        if (updatedRequest == null || !StringUtils.hasText(updatedRequest.getSender())) {
            return "Error: Sender information is required for validation.";
        }
 
        // 3. Check if the sender is authorized to edit the message
        if (!updatedRequest.getSender().equals(message.getSender())) {
            return "Error: You are not authorized to edit this message.";
        }
 
        // 4. Validate and update the message content
        if (StringUtils.hasText(updatedRequest.getContent())) {
            message.setContent(updatedRequest.getContent());
            // If you add an 'isEdited' field to your ChatMessage model, set it here:
            // message.setEdited(true);
        } else {
            return "Error: Updated content cannot be null or empty.";
        }
 
        // 5. Save the updated message to the database
        ChatMessage savedMessage = chatMessageRepository.save(message);
 
        // 6. Prepare the response DTO to be sent over WebSocket
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
                null, // No need to send fileData for a text edit
                savedMessage.getClientId()
                // If you add an 'isEdited' field to your ChatMessageResponse DTO, set it here
        );
 
        // 7. Broadcast the update to the correct destinations
        String type = savedMessage.getType();
        if (type != null) {
            if ("PRIVATE".equals(type)) {
                // For private chats, send to both the sender and receiver's specific user queues.
                // This ensures both parties see the update in real-time.
                if (savedMessage.getSender() != null) {
                    messagingTemplate.convertAndSendToUser(savedMessage.getSender(), "/queue/private", response);
                }
                // Avoid sending a duplicate message if the user is chatting with themselves
                if (savedMessage.getReceiver() != null && !savedMessage.getReceiver().equals(savedMessage.getSender())) {
                    messagingTemplate.convertAndSendToUser(savedMessage.getReceiver(), "/queue/private", response);
                }
            } else if ("TEAM".equals(type) || "DEPARTMENT".equals(type)) {
                // For group chats, broadcast to the public group topic.
                // All members subscribed to this topic will receive the update.
                messagingTemplate.convertAndSend("/topic/team/" + savedMessage.getGroupId(), response);
            } else {
                System.out.println("Warning: Unknown message type for WebSocket broadcast: " + type);
            }
        }
 
        return "Message updated successfully";
    }
}
 
 