package com.app.chat_service.service;

import com.app.chat_service.dto.ChatMessageOverviewDTO;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageOverviewService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageOverviewDTO> getChatMessages(String empId, String chatId) {
        List<ChatMessage> messages;

        // Detect chat type: if chatId starts with TEAM or matches team ID
        if (chatId.startsWith("TEAM")) {
            messages = chatMessageRepository.findTeamChatMessages(chatId);
        } else {
            messages = chatMessageRepository.findPrivateChatMessages(empId, chatId);
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return messages.stream()
                .map(msg -> ChatMessageOverviewDTO.builder()
                        .messageId(msg.getId())
                        .time(msg.getTimestamp() != null ? msg.getTimestamp().format(timeFormatter) : null)
                        .date(msg.getTimestamp() != null ? msg.getTimestamp().format(dateFormatter) : null)
                        .sender(msg.getSender())
                        .receiver(msg.getReceiver() != null ? msg.getReceiver() : msg.getGroupId()) // for team chat
                        .type(msg.getType())
                        .kind(resolveKind(msg))
                        .isSeen(Boolean.toString(msg.isRead()))
                        .content(extractActualContent(msg.getContent()))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private String resolveKind(ChatMessage msg) {
        if (msg.getFileName() != null) return "file";
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
        return content;
    }
}
