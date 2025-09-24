package com.app.chat_service.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor // <-- ADD THIS
@AllArgsConstructor
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
    private Integer duration;
    private ReplyInfoDTO replyTo;
 
    // ======================= REFRESH BUG FIX START =======================
    private Boolean forwarded;
    private String forwardedFrom;
    // ======================= REFRESH BUG FIX END =========================
}