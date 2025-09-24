package com.app.chat_service.redis;

import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.dto.MessageStatusUpdateDTO;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageReadStatus;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.MessageReadStatusRepository;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.OnlineUserService;
import com.app.chat_service.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** The RedisSubscriber will handle the actual delivery to WebSocket clients. **/

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ChatPresenceTracker chatTracker;
    private final TeamService teamService;
    private final OnlineUserService onlineUserService;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository readStatusRepo;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatMessageResponse chatMessage = objectMapper.readValue(body, ChatMessageResponse.class);
            log.info("Received message {} from Redis.", chatMessage.getId());

            if ("PRIVATE".equalsIgnoreCase(chatMessage.getType())) {
                handlePrivateMessage(chatMessage);
            } else if ("TEAM".equalsIgnoreCase(chatMessage.getType())) {
                handleTeamMessage(chatMessage);
            }

        } catch (Exception e) {
            log.error("❌ Error processing Redis message", e);
        }
    }

    private void handlePrivateMessage(ChatMessageResponse chatMessage) {
        String targetUser = chatMessage.getReceiver();
        String senderUser = chatMessage.getSender();

        if (senderUser != null) {
            messagingTemplate.convertAndSendToUser(senderUser, "/queue/private-ack", chatMessage);
        }

        if (targetUser == null) {
            return;
        }

        boolean isWindowOpen = chatTracker.isChatWindowOpen(targetUser, senderUser);

        if (isWindowOpen) {
            // ---- LIVE SEEN LOGIC ----
            chatMessageRepository.findById(chatMessage.getId()).ifPresent(msg -> {
                if (!msg.isRead()) {
                    msg.setRead(true);
                    chatMessageRepository.save(msg);
                    log.info("Marked new message {} as read from RedisSubscriber", msg.getId());
                }
            });

            // 2. Set the 'seen' flag to true in the response
            chatMessage.setSeen(true);

            // 3. Send the message with 'seen' status to the receiver
            messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", chatMessage);

            // 4. Send 'SEEN' status update to the sender
            MessageStatusUpdateDTO statusUpdate = MessageStatusUpdateDTO.builder()
                    .type("STATUS_UPDATE")
                    .status("SEEN")
                    .chatId(targetUser) // the user who saw the message
                    .messageIds(List.of(chatMessage.getId()))
                    .build();
            messagingTemplate.convertAndSendToUser(senderUser, "/queue/private", statusUpdate);
            log.info("Sent SEEN status for new message {} to sender {}", chatMessage.getId(), senderUser);

        } else {
            // ---- DELIVERED LOGIC ----
            // 1. Send the normal message to the receiver (for unread count)
            messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", chatMessage);

            // 2. If the receiver is online, send 'DELIVERED' status to the sender
            if (onlineUserService.isOnline(targetUser)) {
                MessageStatusUpdateDTO statusUpdate = MessageStatusUpdateDTO.builder()
                        .type("STATUS_UPDATE")
                        .status("DELIVERED")
                        .chatId(targetUser)
                        .messageIds(List.of(chatMessage.getId()))
                        .build();
                messagingTemplate.convertAndSendToUser(senderUser, "/queue/private", statusUpdate);
                log.info("Sent DELIVERED status for message {} to sender {}", chatMessage.getId(), senderUser);
            }
        }
    }

    private void handleTeamMessage(ChatMessageResponse chatMessage) {
        String teamId = chatMessage.getGroupId();
        String senderId = chatMessage.getSender();

        //mark messages as 'read' for active users 
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(chatMessage.getId());
        if (messageOpt.isPresent()) {
            ChatMessage messageEntity = messageOpt.get();
            List<String> members = teamService.getEmployeeIdsByTeamId(teamId);

            // Create 'MessageReadStatus' records for members whose chat window is open
            List<MessageReadStatus> newReadStatuses = members.stream()
                .filter(memberId -> !memberId.equals(senderId) && chatTracker.isChatWindowOpen(memberId, teamId))
                .map(activeMemberId -> MessageReadStatus.builder()
                    .chatMessage(messageEntity)
                    .userId(activeMemberId)
                    .readAt(LocalDateTime.now())
                    .build())
                .collect(Collectors.toList());

            if (!newReadStatuses.isEmpty()) {
                readStatusRepo.saveAll(newReadStatuses);
                log.info("Marked new group message {} as read for {} active members.", chatMessage.getId(), newReadStatuses.size());
            }
        } else {
            log.warn("Could not find ChatMessage with ID {} to create read statuses.", chatMessage.getId());
        }

        messagingTemplate.convertAndSend("/topic/team-" + teamId, chatMessage);
        messagingTemplate.convertAndSendToUser(chatMessage.getSender(), "/queue/group-ack", chatMessage);
    }
}
