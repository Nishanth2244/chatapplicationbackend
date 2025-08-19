package com.app.chat_service.service;
 
import com.app.chat_service.dto.PinnedMessageDTO;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.Optional;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePinService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
 
    @Transactional
    public PinnedMessageDTO pinMessage(Long messageId, String userId) {
        log.info("Attempting to pin message {} by user {}", messageId, userId);
 
        ChatMessage messageToPin = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));
 
        String chatId = "PRIVATE".equals(messageToPin.getType()) ? getPrivateChatIdentifier(messageToPin.getSender(), messageToPin.getReceiver()) : messageToPin.getGroupId();
        chatMessageRepository.unpinAllMessagesInChat(chatId, messageToPin.getSender(), messageToPin.getReceiver());
        log.info("Unpinned previous messages in chat: {}", chatId);
 
        messageToPin.setPinned(true);
        messageToPin.setPinnedAt(LocalDateTime.now());
        ChatMessage savedMessage = chatMessageRepository.save(messageToPin);
        log.info("Successfully pinned message ID: {}", savedMessage.getId());
 
        PinnedMessageDTO pinnedDto = mapToPinnedDTO(savedMessage);
 
        PinUnpinEvent event = new PinUnpinEvent("PIN_UPDATE", pinnedDto);
 
        if ("PRIVATE".equals(savedMessage.getType())) {
            messagingTemplate.convertAndSendToUser(savedMessage.getSender(), "/queue/private", event);
            messagingTemplate.convertAndSendToUser(savedMessage.getReceiver(), "/queue/private", event);
        } else {
            messagingTemplate.convertAndSend("/topic/team-" + savedMessage.getGroupId(), event);
        }
 
        return pinnedDto;
    }
 
    @Transactional
    public void unpinMessage(Long messageId, String userId) {
        log.info("Attempting to unpin message {} by user {}", messageId, userId);
 
        ChatMessage messageToUnpin = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));
 
        messageToUnpin.setPinned(false);
        messageToUnpin.setPinnedAt(null);
        chatMessageRepository.save(messageToUnpin);
        log.info("Successfully unpinned message ID: {}", messageToUnpin.getId());
 
        UnpinPayload payload = new UnpinPayload(
            messageToUnpin.getId(),
            messageToUnpin.getGroupId(),
            messageToUnpin.getSender(),
            messageToUnpin.getReceiver()
        );
        PinUnpinEvent event = new PinUnpinEvent("UNPIN_UPDATE", payload);
 
        if ("PRIVATE".equals(messageToUnpin.getType())) {
            messagingTemplate.convertAndSendToUser(messageToUnpin.getSender(), "/queue/private", event);
            messagingTemplate.convertAndSendToUser(messageToUnpin.getReceiver(), "/queue/private", event);
        } else {
            messagingTemplate.convertAndSend("/topic/team-" + messageToUnpin.getGroupId(), event);
        }
    }
 
    @Transactional(readOnly = true)
    public Optional<PinnedMessageDTO> getPinnedMessageForChat(String chatId) {
        return chatMessageRepository.findTopByGroupIdAndPinnedIsTrueOrderByPinnedAtDesc(chatId)
                .map(this::mapToPinnedDTO);
    }
 
    @Transactional(readOnly = true)
    public Optional<PinnedMessageDTO> getPinnedMessageForPrivateChat(String user1, String user2) {
        return chatMessageRepository.findTopBySenderInAndReceiverInAndPinnedIsTrueOrderByPinnedAtDesc(
                        java.util.List.of(user1, user2), java.util.List.of(user1, user2))
                .map(this::mapToPinnedDTO);
    }
 
    private String getPrivateChatIdentifier(String sender, String receiver) {
        return sender.compareTo(receiver) > 0 ? sender + "_" + receiver : receiver + "_" + sender;
    }
 
    private String resolveMessageType(ChatMessage msg) {
        if (msg.getFileName() != null && msg.getFileType() != null) {
            if (msg.getFileType().startsWith("image/")) return "image";
            if (msg.getFileType().startsWith("audio/")) return "audio";
            return "file";
        }
        return "text";
    }
 
    private PinnedMessageDTO mapToPinnedDTO(ChatMessage msg) {
        return PinnedMessageDTO.builder()
                .messageId(msg.getId())
                .content(msg.getContent())
                .sender(msg.getSender())
                .receiver(msg.getReceiver())
                .groupId(msg.getGroupId())
                .type(msg.getType())
                .fileName(msg.getFileName())
                .fileType(msg.getFileType())
                .messageType(resolveMessageType(msg))
                .pinnedAt(msg.getPinnedAt())
                .build();
    }
}
 
@Data
@AllArgsConstructor
class PinUnpinEvent {
    private String type;
    private Object payload;
}
 
@Data
@AllArgsConstructor
class UnpinPayload {
    private Long messageId;
    private String groupId;
    private String senderId;
    private String receiverId;
}
 