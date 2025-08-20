package com.app.chat_service.dto;

import lombok.Data;

@Data
public class ClearChatRequest {
    private String userId;  // who cleared chat
    private String chatId;  // groupId or peerId
}
