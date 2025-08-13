package com.app.chat_service.dto;
 
import lombok.Builder;
import lombok.Data;
 
@Data
@Builder
public class ChatMessageOverviewDTO {
    private Long messageId;
    private String time;
    private String sender;
    private String receiver;
    private String date;
    private String type;
    private String kind;
    private String isSeen;
    private String content;
 
    // --- ADDED FIELDS ---
    private String fileName;
    private String fileType;
    private Long fileSize; // Use Long for file size in bytes
}
 