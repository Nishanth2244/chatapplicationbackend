package com.app.chat_service.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String sender;
    private String receiver;     // optional
    private String groupId;      // optional
    private String type;         // "PRIVATE", "TEAM", "DEPARTMENT"
    private String content;
}
