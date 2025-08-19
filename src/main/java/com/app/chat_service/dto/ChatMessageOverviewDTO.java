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
    private String fileName;
    private String fileType;
    private Long fileSize;
    private ReplyInfoDTO replyTo;
 
    // ======================= REFRESH BUG FIX START =======================
    private Boolean forwarded;
    private String forwardedFrom;
    // ======================= REFRESH BUG FIX END =========================
}