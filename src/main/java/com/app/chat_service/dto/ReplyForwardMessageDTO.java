package com.app.chat_service.dto;
 
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
 
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // This prevents the "Unrecognized field" error
public class ReplyForwardMessageDTO {
    private String sender;
    private String content;          // Used only for replies
    private String clientId;         // For optimistic UI tracking
 
    // For Replies
    private Long replyToMessageId;
    private String receiver;         // The receiver of the reply
    private String groupId;          // The group of the reply
    private String type;             // The type of chat for the reply
 
    // For Forwards
    private Long forwardMessageId;
    private List<ForwardTarget> forwardTo; // A list of chats to forward the message to
}
 