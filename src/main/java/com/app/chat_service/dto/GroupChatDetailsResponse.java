package com.app.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupChatDetailsResponse {
    private String groupId;
    private String teamName;
    private String teamProfile;        // <-- add this
    private List<EmployeeDTO> groupMembers;
    private List<ChatMessageResponse> messages;
}

