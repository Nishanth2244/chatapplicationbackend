package com.app.chat_service.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatPreviewDTO {
    private String chatId;
    private String groupName;      // for group chat
    private String employeeName;   // for private chat
    private String lastMessage;
    private String profile;
    private int unreadMessageCount;
    private boolean isOnline;
    private LocalDateTime lastSeen;
}
