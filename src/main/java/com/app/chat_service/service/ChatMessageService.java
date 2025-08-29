package com.app.chat_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.chat_service.dto.EmployeeTeamResponse;
import com.app.chat_service.dto.MessageStatusUpdateDTO;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageReadStatus;
import com.app.chat_service.model.employee_details;
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
    private final ClearedChatService clearedChatService;
    private final EmployeeDetailsService employeeDetailsService;
    
    
    public List<Map<String, Object>> getChattedEmployeesInSameTeam(String employeeId) {
        List<Map<String, Object>> allChats = new ArrayList<>();
        
        // ✅ Add groups
        List<TeamResponse> teams = Optional.ofNullable(teamService.getTeamsByEmployeeId(employeeId))
                .orElse(Collections.emptyList());
        for (TeamResponse team : teams) {
            allChats.add(buildGroupPreview(team, employeeId));
        }
        

        Set<String> privateChatIds = new HashSet<>();
        privateChatIds.addAll(chatRepo.findDistinctReceiversBySender(employeeId));
        privateChatIds.addAll(chatRepo.findDistinctSendersByReceiver(employeeId));

        // remove self
        privateChatIds.remove(employeeId);
        
        log.info("Step 1");

        for (String otherId : privateChatIds) {
                // ✅ Skip invalid/system IDs
	        if (otherId == null || otherId.isBlank() ||
	                "pin".equalsIgnoreCase(otherId) ||
	                "deleteforeveryone".equalsIgnoreCase(otherId) ||
	                "edit".equalsIgnoreCase(otherId)) {
	        	log.info("Step 2");
	            continue;
	        }
	
	        employee_details response = employeeDetailsService.getEmployeeById(otherId);
	
	        EmployeeTeamResponse emp = new EmployeeTeamResponse();
	        emp.setEmployeeId(response.getEmployeeId());
	        emp.setDisplayName(response.getEmployeeName());	        emp.setProfilelink(response.getProfileLink());
	        allChats.add(buildPrivatePreview(emp, employeeId));
	        log.info("profile link {}",response.getProfileLink());
	        log.info("done");
	        }
        return allChats;
    }

    private Map<String, Object> buildGroupPreview(TeamResponse team, String employeeId) {
        LocalDateTime clearedAt = clearedChatService.getClearedAt(employeeId, team.getTeamId());

     // Fetch only the last message in GroupChat
        Optional<ChatMessage> lastMsgOpt = chatRepo.findTopByGroupIdAndTypeAndTimestampAfterOrderByTimestampDesc(
                team.getTeamId(), "TEAM", clearedAt);

        ChatMessage lastMessage = lastMsgOpt.orElse(null);

        // Use the new query directly for unread count
        // This gives accurate count and also fixes refresh bug
        long unreadCount = chatRepo.countUnreadMessagesForUserInGroup(employeeId, team.getTeamId(), clearedAt);

        Map<String, Object> groupChat = new HashMap<>();
        groupChat.put("chatType", "GROUP");
        groupChat.put("chatId", team.getTeamId());
        groupChat.put("groupName", team.getTeamName());

        if (lastMessage != null) {
            groupChat.put("lastMessage", lastMessage.getContent());
            groupChat.put("lastSeen", lastMessage.getTimestamp());
            groupChat.put("LastMessageSenderId", lastMessage.getSender());
            groupChat.put("lastMessageType", lastMessage.getType());
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
     
        Optional<ChatMessage> lastMsgOpt = chatRepo.findTopByTypeAndSenderInAndReceiverInAndTimestampAfterOrderByTimestampDesc(
                "PRIVATE",
                List.of(employeeId, chatPartnerId),
                List.of(employeeId, chatPartnerId),
                clearedAt
        );
     
        long unreadCount = chatRepo.countUnreadPrivateMessages(employeeId, chatPartnerId, clearedAt);
        if (chatPresenceTracker.isChatWindowOpen(employeeId, chatPartnerId)) {
            markMessagesAsRead(employeeId, chatPartnerId);
            unreadCount = 0;
        }
     
        Map<String, Object> privateChat = new HashMap<>();
        privateChat.put("chatType", "PRIVATE");
        privateChat.put("chatId", chatPartnerId);
        privateChat.put("employeeName", emp.getDisplayName());
     
        if (lastMsgOpt.isPresent()) {
            ChatMessage lastMessage = lastMsgOpt.get();
            privateChat.put("lastMessage", lastMessage.getContent());
            privateChat.put("lastSeen", lastMessage.getTimestamp());
            privateChat.put("lastMessageSenderId", lastMessage.getSender());
            privateChat.put("lastMessageType", lastMessage.getType());  
        } else {
            privateChat.put("lastMessage", "Chat cleared");
            privateChat.put("lastSeen", clearedAt);
        }
     
        privateChat.put("profile", emp.getProfilelink());
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

        List<Long> messageIds = messagesToUpdate.stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toList());

        MessageStatusUpdateDTO statusUpdate = MessageStatusUpdateDTO.builder()
                .type("STATUS_UPDATE")
                .status("SEEN")
                .chatId(userId)
                .messageIds(messageIds)
                .build();

        messagingTemplate.convertAndSendToUser(chatPartnerId, "/queue/private", statusUpdate);
        log.info("Sent SEEN status update for {} messages to sender {}", messageIds.size(), chatPartnerId);

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
