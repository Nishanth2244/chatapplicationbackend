package com.app.chat_service.dto;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyInfoDTO {
    private String senderId;
    private String content;
    private Long originalMessageId;
    private String type; // Original message type (e.g., "text", "image")
}  