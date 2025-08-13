package com.app.chat_service.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.chat_service.dto.EmployeeTeamResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageReadStatus;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.MessageReadStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatRepo;
    private final TeamService teamService;
    private final OnlineUserService onlineUserService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageReadStatusRepository readStatusRepo;

    /**
     * Returns a merged list of all chats (group + private) for a user's sidebar
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChattedEmployeesInSameTeam(String employeeId) {
        List<Map<String, Object>> allChats = new ArrayList<>();

        // 1️⃣ Add group chats
        List<TeamResponse> teams = Optional.ofNullable(teamService.getTeamsByEmployeeId(employeeId))
                .orElse(Collections.emptyList());
        for (TeamResponse team : teams) {
            allChats.add(buildGroupPreview(team, employeeId));
        }

        // 2️⃣ Identify private chat partners
        Set<String> privateChatIds = new HashSet<>();
        chatRepo.findBySender(employeeId)
                .forEach(msg -> Optional.ofNullable(msg.getReceiver()).ifPresent(privateChatIds::add));
        chatRepo.findByReceiver(employeeId)
                .forEach(msg -> Optional.ofNullable(msg.getSender()).ifPresent(privateChatIds::add));
        privateChatIds.remove(employeeId);

        // 3️⃣ Build map of all employees from user's teams
        Map<String, EmployeeTeamResponse> employeeMap = new HashMap<>();
        List<TeamResponse> allTeamsWithEmployees = Optional.ofNullable(teamService.getEmployeesInAllTeamsOf(employeeId))
                .orElse(Collections.emptyList());
        for (TeamResponse team : allTeamsWithEmployees) {
            Optional.ofNullable(team.getEmployees())
                    .ifPresent(emps -> emps.forEach(emp -> employeeMap.put(emp.getEmployeeId(), emp)));
        }

        // 4️⃣ Add private chat previews
        for (String otherId : privateChatIds) {
            EmployeeTeamResponse emp = employeeMap.get(otherId);
            if (emp != null) {
                allChats.add(buildPrivatePreview(emp, employeeId));
            }
        }

        return allChats;
    }

    /**
     * Builds group chat preview for sidebar
     */
    private Map<String, Object> buildGroupPreview(TeamResponse team, String employeeId) {
        List<ChatMessage> messages = chatRepo.findByGroupIdAndType(team.getTeamId(), "TEAM");
        ChatMessage lastMessage = getLastMessage(messages);

        // Calculate unread count based on read-status table
        Set<Long> readMessageIds = readStatusRepo.findReadMessageIdsByUserIdAndGroupId(employeeId, team.getTeamId());
        long unreadCount = messages.stream()
                .filter(msg -> !employeeId.equals(msg.getSender()) && !readMessageIds.contains(msg.getId()))
                .count();

        Map<String, Object> groupChat = new HashMap<>();
        groupChat.put("chatType", "GROUP");
        groupChat.put("chatId", team.getTeamId());
        groupChat.put("groupName", team.getTeamName());
        groupChat.put("lastMessage", lastMessage != null ? lastMessage.getContent() : "");
        groupChat.put("lastSeen", lastMessage != null ? lastMessage.getTimestamp() : null);
        groupChat.put("memberCount", team.getEmployees() != null ? team.getEmployees().size() : 0);
        groupChat.put("unreadMessageCount", unreadCount);
        groupChat.put("isOnline", null); // Groups don't have online status
        return groupChat;
    }

    /**
     * Builds private chat preview for sidebar
     */
    private Map<String, Object> buildPrivatePreview(EmployeeTeamResponse emp, String employeeId) {
        String chatPartnerId = emp.getEmployeeId();

        List<ChatMessage> messages = chatRepo.findBySenderAndReceiverOrReceiverAndSender(
                employeeId, chatPartnerId, employeeId, chatPartnerId);

        long unreadCount = chatRepo.countUnreadPrivateMessages(chatPartnerId, employeeId);

        Optional<ChatMessage> lastMsgOpt = messages.stream()
                .filter(m -> "PRIVATE".equalsIgnoreCase(m.getType()))
                .max(Comparator.comparing(ChatMessage::getTimestamp));

        Map<String, Object> privateChat = new HashMap<>();
        privateChat.put("chatType", "PRIVATE");
        privateChat.put("chatId", chatPartnerId);
        privateChat.put("employeeName", emp.getDisplayName());
        privateChat.put("lastMessage", lastMsgOpt.map(ChatMessage::getContent).orElse(""));
        privateChat.put("lastSeen", lastMsgOpt.map(ChatMessage::getTimestamp).orElse(null));
        privateChat.put("profile", "https://example.com/profiles/" + chatPartnerId + ".jpg");
        privateChat.put("unreadMessageCount", unreadCount);
        privateChat.put("isOnline", onlineUserService.isOnline(chatPartnerId));
        return privateChat;
    }

    /**
     * Returns latest message from a list
     */
    private ChatMessage getLastMessage(List<ChatMessage> messages) {
        return messages.stream()
                .max(Comparator.comparing(ChatMessage::getTimestamp))
                .orElse(null);
    }

    /**
     * Sends sidebar update to a specific user
     */
    @Transactional
    public void broadcastChatOverview(String employeeId) {
        List<Map<String, Object>> overview = getChattedEmployeesInSameTeam(employeeId);
        messagingTemplate.convertAndSendToUser(employeeId, "/queue/sidebar", overview);
    }

    /**
     * Sends sidebar update to all members of a group
     */
    @Transactional
    public void broadcastGroupChatOverview(String groupId) {
        List<String> members = teamService.getEmployeeIdsByTeamId(groupId);
        if (members != null) {
            members.forEach(this::broadcastChatOverview);
        }
    }

    /**
     * Marks all messages between user & partner as read (private chat)
     */
    @Transactional
    public void markMessagesAsRead(String userId, String chatPartnerId) {
        chatRepo.findBySenderAndReceiverOrReceiverAndSender(chatPartnerId, userId, chatPartnerId, userId)
                .stream()
                .filter(m -> m.getReceiver().equals(userId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    chatRepo.save(m);
                });
        broadcastChatOverview(userId);
    }

    /**
     * Marks all group messages as read for a user
     */
    @Transactional
    public void markGroupMessagesAsRead(String userId, String groupId) {
        Set<Long> readMessageIds = readStatusRepo.findReadMessageIdsByUserIdAndGroupId(userId, groupId);

        List<ChatMessage> unreadMessages = chatRepo.findByGroupIdAndType(groupId, "TEAM")
                .stream()
                .filter(msg -> !userId.equals(msg.getSender()) && !readMessageIds.contains(msg.getId()))
                .toList();

        if (!unreadMessages.isEmpty()) {
            List<MessageReadStatus> newReadStatuses = unreadMessages.stream()
                    .map(msg -> MessageReadStatus.builder()
                            .chatMessage(msg)
                            .userId(userId)
                            .readAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

            readStatusRepo.saveAll(newReadStatuses);
        }

        broadcastChatOverview(userId);
    }
}
