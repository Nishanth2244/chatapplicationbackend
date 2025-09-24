package com.app.chat_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingStatusDTO {
    private String senderId;
    private String receiverId;  // For PRIVATE chat
    private String groupId;     // For TEAM/DEPARTMENT
    private String type;        // "PRIVATE", "TEAM", "DEPARTMENT"
    private boolean typing;     // true = typing, false = stopped
}