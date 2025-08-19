package com.app.chat_service.service;
 
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
 
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
 
import com.app.chat_service.dto.EmployeeTeamResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageReadStatus;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.MessageReadStatusRepository;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatMessageService {
 
    private final ChatMessageRepository chatRepo;
    private final TeamService teamService;
    private final OnlineUserService onlineUserService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageReadStatusRepository readStatusRepo;
   
    // ✅ FINAL UNREAD COUNT BUG FIX: Inject the presence tracker.
   
    private final ChatPresenceTracker chatPresenceTracker;
 
 
    public List<Map<String, Object>> getChattedEmployeesInSameTeam(String employeeId) {
        List<Map<String, Object>> allChats = new ArrayList<>();
        List<TeamResponse> teams = Optional.ofNullable(teamService.getTeamsByEmployeeId(employeeId))
                .orElse(Collections.emptyList());
        for (TeamResponse team : teams) {
            allChats.add(buildGroupPreview(team, employeeId));
        }
        Set<String> privateChatIds = new HashSet<>();
        chatRepo.findBySender(employeeId)
                .forEach(msg -> Optional.ofNullable(msg.getReceiver()).ifPresent(privateChatIds::add));
        chatRepo.findByReceiver(employeeId)
                .forEach(msg -> Optional.ofNullable(msg.getSender()).ifPresent(privateChatIds::add));
        privateChatIds.remove(employeeId);
        Map<String, EmployeeTeamResponse> employeeMap = new HashMap<>();
        List<TeamResponse> allTeamsWithEmployees = Optional.ofNullable(teamService.getEmployeesInAllTeamsOf(employeeId))
                .orElse(Collections.emptyList());
        for (TeamResponse team : allTeamsWithEmployees) {
            Optional.ofNullable(team.getEmployees())
                    .ifPresent(emps -> emps.forEach(emp -> employeeMap.put(emp.getEmployeeId(), emp)));
        }
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
 
        // ✅ FINAL UNREAD COUNT BUG FIX: Check presence before counting from DB.
        long unreadCount = 0;
        if (!chatPresenceTracker.isChatWindowOpen(employeeId, team.getTeamId())) {
            Set<Long> readMessageIds = readStatusRepo.findReadMessageIdsByUserIdAndGroupId(employeeId, team.getTeamId());
            unreadCount = messages.stream()
                    .filter(msg -> !employeeId.equals(msg.getSender()) && !readMessageIds.contains(msg.getId()))
                    .count();
        }
 
        Map<String, Object> groupChat = new HashMap<>();
        groupChat.put("chatType", "GROUP");
        groupChat.put("chatId", team.getTeamId());
        groupChat.put("groupName", team.getTeamName());
        groupChat.put("lastMessage", lastMessage != null ? lastMessage.getContent() : "");
        groupChat.put("lastSeen", lastMessage != null ? lastMessage.getTimestamp() : null);
        groupChat.put("memberCount", team.getEmployees() != null ? team.getEmployees().size() : 0);
        groupChat.put("unreadMessageCount", unreadCount);
        groupChat.put("isOnline", null);
        return groupChat;
    }
 
    /**
     * Builds private chat preview for sidebar
     */
    private Map<String, Object> buildPrivatePreview(EmployeeTeamResponse emp, String employeeId) {
        String chatPartnerId = emp.getEmployeeId();
       
        // ✅ FINAL UNREAD COUNT BUG FIX: Check presence before counting from DB.
        long unreadCount = 0;
        if (!chatPresenceTracker.isChatWindowOpen(employeeId, chatPartnerId)) {
            unreadCount = chatRepo.countUnreadPrivateMessages(chatPartnerId, employeeId);
        }
 
        List<ChatMessage> messages = chatRepo.findBySenderAndReceiverOrReceiverAndSender(
                employeeId, chatPartnerId, employeeId, chatPartnerId);
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
 
    private ChatMessage getLastMessage(List<ChatMessage> messages) {
        return messages.stream()
                .max(Comparator.comparing(ChatMessage::getTimestamp))
                .orElse(null);
    }
   
    public void broadcastChatOverview(String employeeId) {
        log.info("Broadcasting chat overview for user: {}", employeeId);
        List<Map<String, Object>> overview = getChattedEmployeesInSameTeam(employeeId);
        messagingTemplate.convertAndSendToUser(employeeId, "/queue/sidebar", overview);
    }
 
    public void broadcastGroupChatOverview(String groupId) {
        List<String> members = teamService.getEmployeeIdsByTeamId(groupId);
        if (members != null) {
            members.forEach(this::broadcastChatOverview);
        }
    }
 
    
    public void markMessagesAsRead(String userId, String chatPartnerId) {
        List<ChatMessage> messagesToUpdate = chatRepo.findBySenderAndReceiverAndReadIsFalse(chatPartnerId, userId);
 
        if (!messagesToUpdate.isEmpty()) {
            log.info("Marking {} private messages as read for user {}", messagesToUpdate.size(), userId);
            messagesToUpdate.forEach(message -> message.setRead(true));
            chatRepo.saveAll(messagesToUpdate);
        }
 
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaction committed for private read status. Broadcasting update to {}", userId);
                broadcastChatOverview(userId);
            }
        });
    }
 
    public void markGroupMessagesAsRead(String userId, String groupId) {
        Set<Long> readMessageIds = readStatusRepo.findReadMessageIdsByUserIdAndGroupId(userId, groupId);
 
        List<ChatMessage> unreadMessages = chatRepo.findByGroupIdAndType(groupId, "TEAM")
                .stream()
                .filter(msg -> !userId.equals(msg.getSender()) && !readMessageIds.contains(msg.getId()))
                .toList();
 
        if (!unreadMessages.isEmpty()) {
            log.info("Marking {} group messages as read for user {} in group {}", unreadMessages.size(), userId, groupId);
            List<MessageReadStatus> newReadStatuses = unreadMessages.stream()
                    .map(msg -> MessageReadStatus.builder()
                            .chatMessage(msg)
                            .userId(userId)
                            .readAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());
            readStatusRepo.saveAll(newReadStatuses);
        }
 
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaction committed for group read status. Broadcasting update to {}", userId);
                broadcastChatOverview(userId);
            }
        });
    } 
}