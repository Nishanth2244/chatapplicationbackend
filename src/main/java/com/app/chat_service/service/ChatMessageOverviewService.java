package com.app.chat_service.service;
 
import com.app.chat_service.dto.ChatMessageOverviewDTO;
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
                // --- MODIFIED LOGIC STARTS HERE ---
                boolean isFile = msg.getFileName() != null;
 
                return ChatMessageOverviewDTO.builder()
                    .messageId(msg.getId())
                    .time(msg.getTimestamp() != null ? msg.getTimestamp().format(timeFmt) : null)
                    .date(msg.getTimestamp() != null ? msg.getTimestamp().format(dateFmt) : null)
                    .sender(msg.getSender())
                    .receiver(msg.getReceiver() != null ? msg.getReceiver() : msg.getGroupId())
                    .type(msg.getType())
                    .kind(resolveKind(msg)) // Determines if it's 'image', 'audio', 'file' etc.
                    .isSeen(Boolean.toString(msg.isRead()))
                   
                    // If it's a file, set content to messageId for the frontend to build the URL.
                    // Otherwise, use the actual text content.
                    .content(isFile ? msg.getId().toString() : extractActualContent(msg.getContent()))
                   
                    // Add the new file-related fields
                    .fileName(msg.getFileName())
                    .fileType(msg.getFileType())
                    .fileSize(msg.getFileData() != null ? (long) msg.getFileData().length : 0L)
                    .build();
                // --- MODIFIED LOGIC ENDS HERE ---
            })
            .collect(Collectors.toList());
    }
 
    private boolean isTeamId(String chatId) {
        return chatId != null && chatId.toUpperCase().startsWith("TEAM");
    }
 
    private String resolveKind(ChatMessage msg) {
        if (msg.getFileName() != null) {
            String fileType = msg.getFileType();
            if (fileType != null) {
                if (fileType.startsWith("image/")) return "image";
                if (fileType.startsWith("audio/")) return "audio";
                if (fileType.startsWith("video/")) return "video";
            }
            return "file"; // Default for other files
        }
        String content = msg.getContent();
        if (content == null) return "send";
        if (content.startsWith("REPLY:")) return "reply";
        if (content.startsWith("FWD:")) return "forward";
        return "send";
    }
 
    private String extractActualContent(String content) {
        if (content == null) return "";
        if (content.startsWith("REPLY:") || content.startsWith("FWD:")) {
            return content.substring(content.indexOf(':') + 1).trim();
        }
        // For file messages from old logic, the content might be "File: filename.pdf".
        // We can clean that up, although the new logic sets content to messageId.
        if (content.startsWith("File:")) {
             return content; // Or return an empty string if you prefer
        }
        return content;
    }
}
 