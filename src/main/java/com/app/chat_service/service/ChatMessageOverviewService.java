package com.app.chat_service.service;
 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.chat_service.dto.ChatMessageOverviewDTO;
import com.app.chat_service.dto.ReplyInfoDTO;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageAction;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.MessageActionRepository;

import lombok.RequiredArgsConstructor;
 
@Service
@RequiredArgsConstructor
public class ChatMessageOverviewService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final MessageActionRepository messageActionRepository;
    // Ee kotha service ni inject chesanu
    private final ClearedChatService clearedChatService;
 
    @Transactional(readOnly = true)
    public List<ChatMessageOverviewDTO> getChatMessages(String empId, String chatId) {
       
        // Chat clear chesina time ni theesukuntunnam
        LocalDateTime clearedAt = clearedChatService.getClearedAt(empId, chatId);
 
        List<ChatMessage> messages;
        if (isTeamId(chatId)) {
            // Paatha method badulu, ee kotha method ni use chesthunnam
            messages = chatMessageRepository.findTeamChatMessagesAfter(chatId, clearedAt);
        } else {
            // Paatha method badulu, ee kotha method ni use chesthunnam
            messages = chatMessageRepository.findPrivateChatMessagesAfter(empId, chatId, clearedAt);
        }
 
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toList());
        List<MessageAction> actions = messageActionRepository
                .findDeleteActionsForUser(messageIds, empId);
        Set<Long> hiddenMessageIds = actions.stream()
                .map(MessageAction::getMessageId)
                .collect(Collectors.toSet());
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return messages.stream()
            .filter(msg -> !hiddenMessageIds.contains(msg.getId()))
            .map(msg -> {
                boolean isFile = msg.getFileName() != null;
                ReplyInfoDTO replyInfo = null;
                if (msg.getReplyToMessage() != null) {
                    ChatMessage original = msg.getReplyToMessage();
                    String originalMessageType = "text";
                    if (original.getFileName() != null && original.getFileType() != null) {
                        if (original.getFileType().startsWith("image/")) originalMessageType = "image";
                        else if (original.getFileType().startsWith("audio/")) originalMessageType = "audio";
                        else originalMessageType = "file";
                    }
                    replyInfo = ReplyInfoDTO.builder()
                        .senderId(original.getSender())
                        .content(msg.getReplyPreviewContent())
                        .originalMessageId(original.getId())
                        .type(originalMessageType)
                        .build();
                }
 
                // ======================= REFRESH BUG FIX START =======================
                return ChatMessageOverviewDTO.builder()
                    .messageId(msg.getId())
                    .time(msg.getTimestamp() != null ? msg.getTimestamp().format(timeFmt) : null)
                    .date(msg.getTimestamp() != null ? msg.getTimestamp().format(dateFmt) : null)
                    .sender(msg.getSender())
                    .receiver(msg.getReceiver() != null ? msg.getReceiver() : msg.getGroupId())
                    .type(msg.getType())
                    .kind(resolveKind(msg))
                    .isSeen(Boolean.toString(msg.isRead()))
                    .content(isFile ? msg.getId().toString() : extractActualContent(msg.getContent()))
                    .fileName(msg.getFileName())
                    .fileType(msg.getFileType())
                    .fileSize(msg.getFileData() != null ? (long) msg.getFileData().length : 0L)
                    .replyTo(replyInfo)
                    .forwarded(msg.getForwarded())
                    .forwardedFrom(msg.getForwardedFrom())
                    .build();
                // ======================= REFRESH BUG FIX END =========================
            })
            .collect(Collectors.toList());
    }
 
    private boolean isTeamId(String chatId) {
        return chatId != null && chatId.toUpperCase().startsWith("TEAM");
    }
 
    private String resolveKind(ChatMessage msg) {
        if (msg.isDeleted()) {
             return "deleted";
        }
        if (msg.getFileName() != null) {
            String fileType = msg.getFileType();
            if (fileType != null) {
                if (fileType.startsWith("image/")) return "image";
                if (fileType.startsWith("audio/")) return "audio";
                if (fileType.startsWith("video/")) return "video";
            }
            return "file";
        }
        return "text";
    }
 
    private String extractActualContent(String content) {
        if (content == null) return "";
        return content;
    }
}
 
 