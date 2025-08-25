package com.app.chat_service.service;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.dto.ForwardTarget;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class ChatForwardService {
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    @Transactional
    public void handleReplyOrForward(ReplyForwardMessageDTO dto) {
        if (dto.getReplyToMessageId() != null) {
            handleReply(dto);
        } else if (dto.getForwardMessageId() != null) {
            handleForward(dto);
        } else {
            throw new IllegalArgumentException("Either replyToMessageId or forwardMessageId must be provided.");
        }
    }
    private void handleReply(ReplyForwardMessageDTO dto) {
        ChatMessage original = messageRepository.findById(dto.getReplyToMessageId())
                .orElseThrow(() -> new RuntimeException("Original message not found"));
        ChatMessage message = new ChatMessage();
        message.setSender(dto.getSender());
        message.setType(dto.getType()); 
        message.setTimestamp(LocalDateTime.now());
        message.setContent(dto.getContent());
        message.setReplyToMessage(original);
        message.setReplyPreviewContent(original.getContent());
        message.setClientId(dto.getClientId());
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            message.setReceiver(dto.getReceiver());
        } else {
            message.setGroupId(dto.getGroupId());
        }
        ChatMessage saved = messageRepository.save(message);
        ChatMessageResponse response = mapToResponse(saved);
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            messagingTemplate.convertAndSendToUser(dto.getReceiver(), "/queue/private", response);
            messagingTemplate.convertAndSendToUser(dto.getSender(), "/queue/private", response);
        } else {
            messagingTemplate.convertAndSend("/topic/team-" + dto.getGroupId(), response);
        }
    }
    private void handleForward(ReplyForwardMessageDTO dto) {
        ChatMessage original = messageRepository.findById(dto.getForwardMessageId())
                .orElseThrow(() -> new RuntimeException("Original message not found"));
        String trueOriginalSender = Boolean.TRUE.equals(original.getForwarded())
                ? original.getForwardedFrom()
                : original.getSender();
        for (ForwardTarget target : dto.getForwardTo()) {
            ChatMessage message = new ChatMessage();
            message.setSender(dto.getSender());
            message.setContent(original.getContent());
            message.setFileName(original.getFileName());
            message.setFileType(original.getFileType());
            message.setFileData(original.getFileData());
            message.setFileSize(original.getFileSize());
            message.setForwarded(true);
            message.setForwardedFrom(trueOriginalSender);
            message.setTimestamp(LocalDateTime.now());
            message.setDuration(original.getDuration());
            if (target.getReceiver() != null) {
                message.setType("PRIVATE");
                message.setReceiver(target.getReceiver());
            } else if (target.getGroupId() != null) {
                message.setType("TEAM");
                message.setGroupId(target.getGroupId());
            } else {
                throw new IllegalArgumentException("Forward target must have either receiver or groupId.");
            }
            ChatMessage saved = messageRepository.save(message);
            ChatMessageResponse response = mapToResponse(saved);
            if ("PRIVATE".equalsIgnoreCase(message.getType())) {
                messagingTemplate.convertAndSendToUser(target.getReceiver(), "/queue/private", response);
            } else {
                messagingTemplate.convertAndSend("/topic/team-" + target.getGroupId(), response);
            }
        }
    }
    private ChatMessageResponse mapToResponse(ChatMessage message) {
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
                null,
                message.getClientId(),
                message.isEdited(),
                message.getDuration()
                );
 
        // ======================= FORWARD FIX START =======================
        response.setForwarded(message.getForwarded());
        response.setForwardedFrom(message.getForwardedFrom());
        // ======================= FORWARD FIX END =========================
        if (message.getReplyToMessage() != null) {
            ChatMessage original = message.getReplyToMessage();
            String originalMessageType = "text";
            if (original.getFileName() != null && original.getFileType() != null) {
                if (original.getFileType().startsWith("image/")) originalMessageType = "image";
                else if (original.getFileType().startsWith("audio/")) originalMessageType = "audio";
                else originalMessageType = "file";
            }
            ChatMessageResponse.ReplyInfo replyInfo = ChatMessageResponse.ReplyInfo.builder()
                .senderId(original.getSender())
                .content(message.getReplyPreviewContent())
                .originalMessageId(original.getId())
                .type(originalMessageType)
                .build();
            response.setReplyTo(replyInfo);
        }
        return response;
    }
}