package com.app.chat_service.dto;
 
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
    private Long fileSize;
    private String type;
    private LocalDateTime timestamp;
    private byte[] fileData;
    private boolean seen;
    private Boolean isDeleted = false;
 
    @JsonProperty("isEdited")
    private boolean isEdited = false;
 
    @JsonProperty("client_id")
    private String clientId;
 
    // ======================= BUG FIX STARTS HERE =======================
    // Reply context kosam ee kotha object ni add chesthunnam.
    private ReplyInfo replyTo;
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplyInfo {
        private String senderId;
        private String content;
        private Long originalMessageId;
        private String type; // Original message type (text, image, etc.)
    }
    // ======================= BUG FIX ENDS HERE =========================
 
    // Main constructor used in the application
    public ChatMessageResponse(Long id, String sender, String receiver, String groupId, String content, String fileName, String fileType, Long fileSize, String type, LocalDateTime timestamp, byte[] fileData, String clientId, boolean isEdited) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.groupId = groupId;
        this.content = content;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.type = type;
        this.timestamp = timestamp;
        this.fileData = fileData;
        this.clientId = clientId;
        this.isEdited = isEdited; // Initialize the new field
    }
 
    // Overloaded constructor for backward compatibility if needed elsewhere
    public ChatMessageResponse(Long id, String sender, String receiver, String groupId, String content, String fileName, String fileType, Long fileSize, String type, LocalDateTime timestamp, byte[] fileData, String clientId) {
         this(id, sender, receiver, groupId, content, fileName, fileType, fileSize, type, timestamp, fileData, clientId, false);
    }
}    