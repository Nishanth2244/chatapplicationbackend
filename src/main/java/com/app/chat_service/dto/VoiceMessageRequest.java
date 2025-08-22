package com.app.chat_service.dto;
 
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
public class VoiceMessageRequest {
    private String sender;
    private String receiver;
    private String groupId;
    private String type; // "PRIVATE" or "TEAM"
    private String clientId;
    private String fileType; // e.g., "audio/webm"
    private String fileName; // e.g., "voice-message-12345.webm"
    private String fileData; // This will contain the Base64 encoded string
    private Integer duration;
}