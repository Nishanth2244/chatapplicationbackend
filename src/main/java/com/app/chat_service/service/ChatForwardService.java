
package com.app.chat_service.service;
 
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
 
import java.time.LocalDateTime;
 
@Service
@RequiredArgsConstructor
public class ChatForwardService {
 
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
 
    public ChatMessageResponse handleReplyOrForward(ReplyForwardMessageDTO dto) {
        ChatMessage message = new ChatMessage();
        message.setSender(dto.getSender());
        message.setType(dto.getType());
        message.setTimestamp(LocalDateTime.now());
 
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            message.setReceiver(dto.getReceiver());
        } else {
            message.setGroupId(dto.getGroupId());
        }
 
        if (dto.getReplyToMessageId() != null) {
            // Reply logic
            ChatMessage original = messageRepository.findById(dto.getReplyToMessageId())
                    .orElseThrow(() -> new RuntimeException("Original message not found"));
            message.setContent(dto.getContent());
            message.setReplyToMessage(original); // ✅ Fix here
            message.setReplyPreviewContent(original.getContent());
        } else if (dto.getForwardMessageId() != null) {
            // Forward logic
            ChatMessage original = messageRepository.findById(dto.getForwardMessageId())
                    .orElseThrow(() -> new RuntimeException("Original message not found"));
            message.setContent(original.getContent());
            message.setFileName(original.getFileName());
            message.setFileType(original.getFileType());
            message.setFileData(original.getFileData());
            message.setForwarded(true); // ✅ Flag as forwarded
            message.setForwardedFrom(original.getSender()); // ✅ Track origin
        } else {
            throw new IllegalArgumentException("Either replyToMessageId or forwardMessageId must be provided.");
        }
 
        ChatMessage saved = messageRepository.save(message);
        ChatMessageResponse response = mapToResponse(saved);
 
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            messagingTemplate.convertAndSendToUser(dto.getReceiver(), "/queue/private", response);
        } else {
            messagingTemplate.convertAndSend("/topic/" + dto.getGroupId(), response);
        }
 
        return response;
    }
 
 
    private ChatMessageResponse mapToResponse(ChatMessage message) {
        return new ChatMessageResponse(
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
    }
}      
