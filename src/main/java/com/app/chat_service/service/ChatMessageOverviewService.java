package com.app.chat_service.service;
 
import com.app.chat_service.dto.ChatMessageOverviewDTO;
import com.app.chat_service.dto.ReplyInfoDTO; // Import the new DTO
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageAction;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.MessageActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
public class ChatMessageOverviewService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final MessageActionRepository messageActionRepository;
 
    @Transactional(readOnly = true)
    public List<ChatMessageOverviewDTO> getChatMessages(String empId, String chatId) {
        List<ChatMessage> messages;
 
        if (isTeamId(chatId)) {
            messages = chatMessageRepository.findTeamChatMessages(chatId);
        } else {
            messages = chatMessageRepository.findPrivateChatMessages(empId, chatId);
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
 
                // ======================= BUG FIX STARTS HERE =======================
                // Message ఒకవేళ reply అయితే, దాని details ని build చేస్తున్నాం.
                ReplyInfoDTO replyInfo = null;
                if (msg.getReplyToMessage() != null) {
                    ChatMessage original = msg.getReplyToMessage();
                    String originalMessageType = "text"; // Default
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
                // ======================= BUG FIX ENDS HERE =========================
 
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
                    .replyTo(replyInfo) // <-- Build చేసిన reply details ని ఇక్కడ set చేస్తున్నాం.
                    .build();
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