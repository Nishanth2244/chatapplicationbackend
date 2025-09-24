package com.app.chat_service.dto;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteNotificationDTO {
    private Long messageId;
    private String groupId;
    private String sender;
    private String receiver;
    private boolean isDeleted;
    private String type; // To inform the frontend this is a delete notification, e.g., "deleted"
}