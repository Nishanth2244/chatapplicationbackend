package com.app.chat_service.dto;
 
import lombok.Data;
 
@Data
public class ForwardTarget {
    private String receiver; // for private messages
    private String groupId;  // for group messages
//    private String type;     // "PRIVATE", "TEAM", etc.
}