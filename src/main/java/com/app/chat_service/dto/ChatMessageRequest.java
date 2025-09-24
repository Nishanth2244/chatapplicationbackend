package com.app.chat_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageRequest {
	@JsonProperty("messageId") // ✅ Ensures JSON field matches exactly
    private Long messageId;
    private String content;
    private String groupId;
    private String sender;
    private String receiver;
    private String type;
    private String kind; // text/file separation

    // File fields
    private String fileName;
    private String fileType;
    private String fileData; // Base64 encoded string

    // Client tracking
    @JsonProperty("client_id")
    private String clientId;
}
