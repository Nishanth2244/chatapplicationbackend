package com.app.chat_service.dto;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedMessageDTO {
 
    private Long messageId;
    private String content;
    private String sender;
    private String fileName;
    private String fileType;
    private String messageType; // "text", "image", "file", etc.
    private LocalDateTime pinnedAt;
 
    private String receiver;
    private String groupId;
    private String type; // "PRIVATE" or "GROUP"
 
}