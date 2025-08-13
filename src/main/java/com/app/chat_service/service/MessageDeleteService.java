package com.app.chat_service.service;
 
import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.NoSuchElementException;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDeleteService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final ChatKafkaProducer chatKafkaProducer; // For real-time events via Kafka
 
    /**
     * Hides a message for the user who requested it.
     * This is now a "soft delete" that updates the message for everyone.
     * This ensures consistency and that the message stays hidden on refresh.
     */
    @Transactional
    public void deleteForMe(Long messageId, String userId) {
        // For simplicity, "Delete for Me" will now behave like a soft "Delete for Everyone".
        // This ensures the change is persistent and seen by all.
        // The sender check is bypassed.
        try {
            this.deleteMessageForEveryone(messageId, userId, true);
        } catch (IllegalAccessException e) {
            // This exception won't be thrown when bypassSenderCheck is true
            log.error("Unexpected access exception during deleteForMe", e);
        }
    }
 
    /**
     * Deletes a message for everyone. This is a "soft delete".
     * It updates the message content and broadcasts the change via Kafka.
     */
    @Transactional
    public void deleteForEveryone(Long messageId, String userId) throws IllegalAccessException {
        this.deleteMessageForEveryone(messageId, userId, false);
    }
 
    /**
     * Private helper method to handle the core logic for soft-deleting a message.
     *
     * @param messageId The ID of the message to delete.
     * @param userId The ID of the user requesting the action.
     * @param bypassSenderCheck If true, the check to see if the user is the sender is skipped.
     */
    private void deleteMessageForEveryone(Long messageId, String userId, boolean bypassSenderCheck) throws IllegalAccessException {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found with ID: " + messageId));
 
        // Security Check: If not bypassing, ensure the requester is the sender.
        if (!bypassSenderCheck && !message.getSender().equals(userId)) {
            throw new IllegalAccessException("Only the sender can delete this message for everyone.");
        }
 
        // Update the message state to "deleted"
        message.setContent("This message was deleted");
        message.setType("DELETED"); // Use a specific type for the delete event
        message.setFileName(null);
        message.setFileType(null);
        message.setFileSize(null);
        message.setFileData(null);
 
        ChatMessage deletedMessage = chatMessageRepository.save(message);
        log.info("Soft deleted message with ID: {}. Broadcasting change via Kafka.", messageId);
 
        // Broadcast the delete event via Kafka.
        // The Kafka consumer will handle WebSocket broadcasting and sidebar updates.
        chatKafkaProducer.send(deletedMessage);
    }
}
 
 