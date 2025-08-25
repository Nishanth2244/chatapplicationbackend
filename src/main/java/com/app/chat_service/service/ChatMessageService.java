package com.app.chat_service.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.chat_service.dto.EmployeeDTO;
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
    private final ChatPresenceTracker chatPresenceTracker;
    private final AllEmployees allEmployees;
    private final ClearedChatService clearedChatService;

    public List<Map<String, Object>> getChattedEmployeesInSameTeam(String employeeId) {
        List<Map<String, Object>> allChats = new ArrayList<>();

        // ✅ Add groups
        List<TeamResponse> teams = Optional.ofNullable(teamService.getTeamsByEmployeeId(employeeId))
                .orElse(Collections.emptyList());
        for (TeamResponse team : teams) {
            allChats.add(buildGroupPreview(team, employeeId));
        }

        // ✅ Collect private chat partners
        Set<String> privateChatIds = new HashSet<>();
        chatRepo.findBySender(employeeId)
                .forEach(msg -> Optional.ofNullable(msg.getReceiver()).ifPresent(privateChatIds::add));
        chatRepo.findByReceiver(employeeId)
                .forEach(msg -> Optional.ofNullable(msg.getSender()).ifPresent(privateChatIds::add));
        privateChatIds.remove(employeeId);

        for (String otherId : privateChatIds) {
            try {
                // ✅ Skip invalid/system IDs
            	  if (otherId == null || otherId.isBlank() ||
            	            "pin".equalsIgnoreCase(otherId) ||
            	            "deleteforeveryone".equalsIgnoreCase(otherId) ||
            	            "edit".equalsIgnoreCase(otherId)) {

            	            continue;
            	        }

                ResponseEntity<EmployeeDTO> response = allEmployees.getEmployeeById(otherId);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    EmployeeDTO empDto = response.getBody();
                    EmployeeTeamResponse emp = new EmployeeTeamResponse();
                    emp.setEmployeeId(empDto.getEmployeeId());
                    emp.setDisplayName(empDto.getDisplayName());
                    allChats.add(buildPrivatePreview(emp, employeeId));
                } else {
                    log.warn("No employee found for ID: {}", otherId);
                }
            } catch (Exception e) {
                log.error("Could not fetch employee details for ID: {}", otherId, e);
            }
        }
        return allChats;
    }

    private Map<String, Object> buildGroupPreview(TeamResponse team, String employeeId) {
        LocalDateTime clearedAt = clearedChatService.getClearedAt(employeeId, team.getTeamId());
        List<ChatMessage> allMessages = chatRepo.findByGroupIdAndType(team.getTeamId(), "TEAM");
        List<ChatMessage> messagesAfterClear = allMessages.stream()
                .filter(msg -> msg.getTimestamp().isAfter(clearedAt))
                .collect(Collectors.toList());

        ChatMessage lastMessage = getLastMessage(messagesAfterClear);

        long unreadCount = 0;
        if (!chatPresenceTracker.isChatWindowOpen(employeeId, team.getTeamId())) {
            Set<Long> readMessageIds = readStatusRepo.findReadMessageIdsByUserIdAndGroupId(employeeId, team.getTeamId());
            unreadCount = messagesAfterClear.stream()
                    .filter(msg -> !employeeId.equals(msg.getSender()) && !readMessageIds.contains(msg.getId()))
                    .count();
        }

        Map<String, Object> groupChat = new HashMap<>();
        groupChat.put("chatType", "GROUP");
        groupChat.put("chatId", team.getTeamId());
        groupChat.put("groupName", team.getTeamName());

        if (lastMessage != null) {
            groupChat.put("lastMessage", lastMessage.getContent());
            groupChat.put("lastSeen", lastMessage.getTimestamp());
        } else {
            groupChat.put("lastMessage", "Chat cleared");
            groupChat.put("lastSeen", clearedAt);
        }

        groupChat.put("memberCount", team.getEmployees() != null ? team.getEmployees().size() : 0);
        groupChat.put("unreadMessageCount", unreadCount);
        groupChat.put("isOnline", null);
        return groupChat;
    }

    private Map<String, Object> buildPrivatePreview(EmployeeTeamResponse emp, String employeeId) {
    String chatPartnerId = emp.getEmployeeId();
    LocalDateTime clearedAt = clearedChatService.getClearedAt(employeeId, chatPartnerId);

    // ✅ Fetch only messages after chat was cleared
    List<ChatMessage> allMessages = chatRepo.findBySenderAndReceiverOrReceiverAndSender(
            employeeId, chatPartnerId, employeeId, chatPartnerId)
            .stream()
            .filter(m -> "PRIVATE".equalsIgnoreCase(m.getType()) && m.getTimestamp().isAfter(clearedAt))
            .collect(Collectors.toList());

    // ✅ Unread count calculation (only messages after clear + not read + not sent by me)
    long unreadCount = allMessages.stream()
            .filter(m -> !m.isRead() && !m.getSender().equals(employeeId))
            .count();

    if (chatPresenceTracker.isChatWindowOpen(employeeId, chatPartnerId)) {
        // reset unread if window is already open
        markMessagesAsRead(employeeId, chatPartnerId);
        unreadCount = 0;
    }

    // ✅ Last message after clear
    Optional<ChatMessage> lastMsgOpt = allMessages.stream()
            .max(Comparator.comparing(ChatMessage::getTimestamp));

    Map<String, Object> privateChat = new HashMap<>();
    privateChat.put("chatType", "PRIVATE");
    privateChat.put("chatId", chatPartnerId);
    privateChat.put("employeeName", emp.getDisplayName());

    if (lastMsgOpt.isPresent()) {
        ChatMessage lastMessage = lastMsgOpt.get();
        privateChat.put("lastMessage", lastMessage.getContent());
        privateChat.put("lastSeen", lastMessage.getTimestamp());
    } else {
        privateChat.put("lastMessage", "Chat cleared");
        privateChat.put("lastSeen", clearedAt);
    }

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

    @Async("taskExecutor")
    public void broadcastOverviewAsynchronously(String senderId, String receiverId, String groupId, String type) {
        broadcastChatOverview(senderId);
        if ("PRIVATE".equalsIgnoreCase(type) && receiverId != null) {
            broadcastChatOverview(receiverId);
        } else if ("TEAM".equalsIgnoreCase(type) && groupId != null) {
            broadcastGroupChatOverview(groupId);
        }
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
