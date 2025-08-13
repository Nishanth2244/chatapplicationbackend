package com.app.chat_service.dto;
 
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {
    private Long id;
    private String sender;
    private String receiver;
    private String groupId;
    private String content;
    private String fileName;
    private String fileType;
    private Long fileSize; // <<< 1. FILESIZE FIELD ADD CHEYANDI
    private String type;
    private LocalDateTime timestamp;
    private byte[] fileData;
    private boolean seen; // For read status
 
    @JsonProperty("client_id")
    private String clientId;
   
    // Overloaded constructor without 'seen' for backward compatibility if needed
    public ChatMessageResponse(Long id, String sender, String receiver, String groupId, String content, String fileName, String fileType, Long fileSize, String type, LocalDateTime timestamp, byte[] fileData, String clientId) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.groupId = groupId;
        this.content = content;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize; // <<< 2. CONSTRUCTOR LO INITIALIZE CHEYANDI
        this.type = type;
        this.timestamp = timestamp;
        this.fileData = fileData;
        this.clientId = clientId;
    }
}
 
 