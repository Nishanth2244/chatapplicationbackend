package com.app.chat_service.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.chat_service.config.OnlineUserService;
import com.app.chat_service.dto.EmployeeTeamResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatRepo;
    private final TeamService teamService;
    private final OnlineUserService onlineUserService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChattedEmployeesInSameTeam(String employeeId) {

        // Final list for both groups & private chats
        List<Map<String, Object>> allChats = new ArrayList<>();

        // Step 1: Get teams for employee (never null)
        List<TeamResponse> teams = teamService.getTeamsByEmployeeId(employeeId);

        // Step 2: Build group chat previews
        for (TeamResponse team : teams) {
            List<ChatMessage> groupMessages = chatRepo.findByGroupIdAndType(team.getTeamId(), "TEAM");

            ChatMessage lastMessage = getLastMessage(groupMessages);
            long unreadCount = countUnreadMessages(groupMessages, employeeId);

            Map<String, Object> groupChat = new HashMap<>();
            groupChat.put("chatType", "GROUP");
            groupChat.put("chatId", team.getTeamId());
            groupChat.put("groupName", team.getTeamName());
            groupChat.put("lastMessage", lastMessage != null ? lastMessage.getContent() : "");
            groupChat.put("profile", "https://example.com/group-profiles/" + team.getTeamId() + ".jpg");
            groupChat.put("unreadMessageCount", unreadCount);
            groupChat.put("isOnline", true);  // Always true for groups
            groupChat.put("lastSeen", lastMessage != null ? lastMessage.getTimestamp() : null);

            allChats.add(groupChat);
        }

        // Step 3: Get unique private chat user IDs
        Set<String> privateChatIds = new HashSet<>();
        chatRepo.findBySender(employeeId).forEach(msg -> {
            if (msg.getReceiver() != null) privateChatIds.add(msg.getReceiver());
        });
        chatRepo.findByReceiver(employeeId).forEach(msg -> {
            if (msg.getSender() != null) privateChatIds.add(msg.getSender());
        });
        privateChatIds.remove(employeeId);

        // Step 4: Fetch all employees in all teams of this employee for mapping
        List<TeamResponse> allTeamsWithEmployees = teamService.getEmployeesInAllTeamsOf(employeeId);

        Map<String, EmployeeTeamResponse> employeeMap = new HashMap<>();
        for (TeamResponse team : allTeamsWithEmployees) {
            if (team.getEmployees() == null) continue;
            for (EmployeeTeamResponse emp : team.getEmployees()) {
                employeeMap.put(emp.getEmployeeId(), emp);
            }
        }

        // Step 5: Build private chat previews
        for (String otherId : privateChatIds) {
            List<ChatMessage> privateMessages = chatRepo
                .findBySenderAndReceiverOrReceiverAndSender(employeeId, otherId, employeeId, otherId);

            ChatMessage lastMessage = getLastMessage(privateMessages);
            long unreadCount = countUnreadMessages(privateMessages, employeeId);

            EmployeeTeamResponse emp = employeeMap.get(otherId);
            if (emp != null) {
                Map<String, Object> privateChat = new HashMap<>();
                privateChat.put("chatType", "PRIVATE");
                privateChat.put("chatId", emp.getEmployeeId());
                privateChat.put("employeeName", emp.getDisplayName());
                privateChat.put("lastMessage", lastMessage != null ? lastMessage.getContent() : "");
                privateChat.put("profile", "https://example.com/profiles/" + emp.getEmployeeId() + ".jpg");
                privateChat.put("unreadMessageCount", unreadCount);
                privateChat.put("isOnline", onlineUserService.isOnline(emp.getEmployeeId())); // Online status
                privateChat.put("lastSeen", lastMessage != null ? lastMessage.getTimestamp() : null);

                allChats.add(privateChat);
            }
        }

        return allChats;  // Final merged list
    }

    private ChatMessage getLastMessage(List<ChatMessage> messages) {
        return messages.stream()
                .max(Comparator.comparing(ChatMessage::getTimestamp))
                .orElse(null);
    }

    private long countUnreadMessages(List<ChatMessage> messages, String employeeId) {
        return messages.stream()
                .filter(msg -> employeeId.equals(msg.getReceiver()) && !msg.isRead())
                .count();
    }
}
