package com.app.chat_service.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

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
    private String type;
    private LocalDateTime timestamp;
}
