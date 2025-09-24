package com.app.chat_service.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final AllEmployees allEmployees;    
    
    public List<Map<String, Object>> getChattedEmployeesInSameTeam(String employeeId, int page, int size) {
    	
        // 1. Fetch all groups and create previews
        List<TeamResponse> teams = Optional.ofNullable(teamService.getTeamsByEmployeeId(employeeId))
                .orElse(Collections.emptyList());
        List<Map<String, Object>> groupChats = teams.stream()
                .map(team -> buildGroupPreview(team, employeeId))
                .collect(Collectors.toList());
        log.info("Step 1");
     
        // 2. Fetch all unique private chat partners
        Set<String> privateChatIds = new HashSet<>();
        privateChatIds.addAll(chatRepo.findDistinctReceiversBySender(employeeId));
        privateChatIds.addAll(chatRepo.findDistinctSendersByReceiver(employeeId));
        privateChatIds.remove(employeeId);
     
        log.info("Step 2");
        // 3. Create previews for all private chats
        List<Map<String, Object>> privateChats = privateChatIds.stream()
                .map(otherId -> {
                    if (otherId == null || otherId.isBlank() || "pin".equalsIgnoreCase(otherId) || "deleteforeveryone".equalsIgnoreCase(otherId) || "edit".equalsIgnoreCase(otherId)) {
                        return null;
                    }
                    employee_details response = allEmployees.getEmployeeById(otherId);
                    log.info("Employee details{}", response);
                    if (response == null) return null;
     
                    EmployeeTeamResponse emp = new EmployeeTeamResponse();
                    emp.setEmployeeId(response.getEmployeeId());
                    emp.setDisplayName(response.getEmployeeName());
                    emp.setProfilelink(response.getProfileLink());
                    return buildPrivatePreview(emp, employeeId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.info("Done")
        ;
        // 4. Combine both lists into one
        List<Map<String, Object>> allChats = Stream.concat(groupChats.stream(), privateChats.stream())
                .collect(Collectors.toList());
     
        // 5. Sort the entire combined list by the last message timestamp (newest first)
        allChats.sort(Comparator.comparing((Map<String, Object> chat) -> {
            Object lastSeen = chat.get("lastSeen");
            if (lastSeen instanceof LocalDateTime) {
                return (LocalDateTime) lastSeen;
            }
            return LocalDateTime.MIN; // Put chats with no messages at the end
        }).reversed());
     
        // 6. Apply manual pagination to the final sorted list
        int start = page * size;
        int end = Math.min(start + size, allChats.size());
     
        if (start >= allChats.size()) {
            return Collections.emptyList(); // Return empty list if page number is out of bounds
        }
     
        return allChats.subList(start, end);
    }

    private Map<String, Object> buildGroupPreview(TeamResponse team, String employeeId) {
        LocalDateTime clearedAt = clearedChatService.getClearedAt(employeeId, team.getTeamId());

        Optional<ChatMessage> lastMsgOpt = chatRepo.findTopByGroupIdAndTypeAndTimestampAfterOrderByTimestampDesc(
                team.getTeamId(), "TEAM", clearedAt);

        ChatMessage lastMessage = lastMsgOpt.orElse(null);
        
        long unreadCount; 

        if (chatPresenceTracker.isChatWindowOpen(employeeId, team.getTeamId())) {
            markGroupMessagesAsRead(employeeId, team.getTeamId()); // Database lo update cheyadaniki
            unreadCount = 0; 
        } else {
            unreadCount = chatRepo.countUnreadMessagesForUserInGroup(employeeId, team.getTeamId(), clearedAt);
        }

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


    public void broadcastChatOverview(String employeeId) {
        log.info("Broadcasting chat overview for user: {}", employeeId);
        List<Map<String, Object>> overview = getChattedEmployeesInSameTeam(employeeId, 0, 15); 
        messagingTemplate.convertAndSendToUser(employeeId, "/queue/sidebar", overview);
    }


    @Async("asyncTaskExecutor")
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
