package com.app.chat_service.dto;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.util.List;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusUpdateDTO {
    private String type; // "STATUS_UPDATE"
    private String status; // "DELIVERED" or "SEEN"
    private String chatId; // The chat where the update happened (sender's ID for private chat)
    private List<Long> messageIds; // List of message IDs that were updated
}  