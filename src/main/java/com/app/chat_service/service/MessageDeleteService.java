package com.app.chat_service.service;
 
import com.app.chat_service.dto.DeleteNotificationDTO;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageAction;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.MessageActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.NoSuchElementException;
import java.util.Objects;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDeleteService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final MessageActionRepository messageActionRepository;
    // private final ChatKafkaProducer chatKafkaProducer; // We will bypass Kafka for direct notification
   
    // *** BUG FIX: Injected SimpMessagingTemplate for direct WebSocket communication ***
    private final SimpMessagingTemplate simpMessagingTemplate;
 
    @Transactional
    public void deleteForMe(Long messageId, String userId) {
        log.info("Recording 'DELETE_FOR_ME' action for message ID {} by user {}", messageId, userId);
       
        chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found with ID: " + messageId));
 
        MessageAction action = MessageAction.builder()
                .messageId(messageId)
                .userId(userId)
                .actionType("DELETE_ME")
                .build();
 
        messageActionRepository.save(action);
        log.info("✅ Recorded 'DELETE_FOR_ME' action for message ID {} by user {}", messageId, userId);
    }
 
    /**
     * Deletes a message for everyone. This is a "soft delete".
     * It updates the message state and broadcasts the change directly via WebSocket.
     */
    @Transactional
    public void deleteForEveryone(Long messageId, String userId) {
        log.info("Message deleted for everyone. Message ID: {}, Requester: {}", messageId, userId);
 
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found with ID: " + messageId));
 
        // Security Check: Ensure the requester is the sender.
        if (!Objects.equals(message.getSender(), userId)) {
            log.warn("Unauthorized delete attempt for message ID {} by user {}", messageId, userId);
            throw new IllegalStateException("Only the sender can delete this message for everyone.");
        }
 
        // 1. Update the message state for soft deletion
        message.setDeleted(true);
        message.setContent("This message was deleted");
        message.setFileName(null);
        message.setFileType(null);
        message.setFileSize(null);
        message.setFileData(null);
        chatMessageRepository.save(message);
        log.info("✅ Soft deleted message ID: {}", messageId);
 
        // 2. Create a notification payload for the frontend
        DeleteNotificationDTO notification = DeleteNotificationDTO.builder()
                .messageId(message.getId())
                .isDeleted(true)
                .type("deleted") // This type is crucial for the frontend to identify the action
                .build();
 
        // 3. Broadcast the notification to the correct destination
        if ("TEAM".equalsIgnoreCase(message.getType()) && message.getGroupId() != null) {
            notification.setGroupId(message.getGroupId());
            String destination = "/topic/team-" + message.getGroupId();
            simpMessagingTemplate.convertAndSend(destination, notification);
            log.info("Sent delete notification to group topic: {}", destination);
 
        } else if ("PRIVATE".equalsIgnoreCase(message.getType())) {
            notification.setSender(message.getSender());
            notification.setReceiver(message.getReceiver());
           
            // Send to the receiver's private queue
            simpMessagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/private", notification);
            // Send to the sender's private queue (to confirm deletion on their other devices)
            simpMessagingTemplate.convertAndSendToUser(message.getSender(), "/queue/private", notification);
            log.info("Sent delete notification to user queues for sender: {} and receiver: {}", message.getSender(), message.getReceiver());
        }
    }
}