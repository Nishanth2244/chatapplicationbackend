package com.app.chat_service.dto;
 
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
/**
 * This is a dedicated Data Transfer Object (DTO) for sending acknowledgements
 * back to the client. It ensures the JSON structure is exactly what the frontend
 * expects, especially the "client_id" field.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageAckDTO {
 
    @JsonProperty("messageId")
    private Long messageId;
 
    private String sender;
    private String receiver;
    private String content;
    private String type;
    private String groupId;
    private LocalDateTime timestamp;
 
    @JsonProperty("isDeleted")
    private boolean isDeleted;
 
    // This is the most important field.
    // The name in JSON will be "client_id" because of the @JsonProperty annotation.
    @JsonProperty("client_id")
    private String clientId;
 
    // Adding other fields the frontend might expect to avoid issues
    private String fileName;
    private Long fileSize;
 
    @JsonProperty("isEdited")
    private boolean isEdited;
}