package com.app.chat_service.kakfa;

import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageReadStatus;
import com.app.chat_service.redis.RedisPublisherService;
import com.app.chat_service.repo.MessageReadStatusRepository;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatKafkaConsumer {

    private final RedisPublisherService redisPublisher;
    private final ChatMessageService chatMessageService;
    private final ChatPresenceTracker presenceTracker;
    private final MessageReadStatusRepository readStatusRepo;
    private final TeamService teamService;

    @KafkaListener(
            topics = {"team", "department", "private"},
            groupId = "chat-group",
            containerFactory = "chatKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ChatMessage incomingMessage) {
        long startTime = System.currentTimeMillis();

        // Deleted message check
        boolean isDeleted = "This message was deleted".equalsIgnoreCase(incomingMessage.getContent());
        if (isDeleted) {
            log.info("🗑️ Deleted message received to consumer. ID: {}", incomingMessage.getId());
        } else {
            log.info("📥 Message received to consumer. ID: {}", incomingMessage.getId());
        }

        boolean isPrivateRead = false;

        if ("PRIVATE".equalsIgnoreCase(incomingMessage.getType())) {
            String receiverId = incomingMessage.getReceiver();
            String senderId = incomingMessage.getSender();

            if (receiverId != null && presenceTracker.isChatWindowOpen(receiverId, senderId)) {
                isPrivateRead = true;
            }

        } else if ("TEAM".equalsIgnoreCase(incomingMessage.getType())) {
            List<String> memberIds = teamService.getEmployeeIdsByTeamId(incomingMessage.getGroupId());

            List<MessageReadStatus> readStatusesToCreate = memberIds.stream()
                    .filter(memberId -> !memberId.equals(incomingMessage.getSender()) &&
                                        presenceTracker.isChatWindowOpen(memberId, incomingMessage.getGroupId()))
                    .map(memberId -> MessageReadStatus.builder()
                            .chatMessage(incomingMessage)
                            .userId(memberId)
                            .readAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

            if (!readStatusesToCreate.isEmpty()) {
                readStatusRepo.saveAll(readStatusesToCreate);
                log.info("✅ Marked message as read for {} active group members.", readStatusesToCreate.size());
            }
        }

        // Build response
        ChatMessageResponse response = new ChatMessageResponse(
                incomingMessage.getId(),
                incomingMessage.getSender(),
                incomingMessage.getReceiver(),
                incomingMessage.getGroupId(),
                incomingMessage.getContent(),
                incomingMessage.getFileName(),
                incomingMessage.getFileType(),
                incomingMessage.getFileSize(),
                incomingMessage.getType(),
                incomingMessage.getTimestamp(),
                null,
                incomingMessage.getClientId(),
                incomingMessage.getDuration()
        );

        if (incomingMessage.getFileName() != null) {             
            String fileUrl = "/api/chat/file/" + incomingMessage.getId(); 
            response.setFileUrl(fileUrl); 
        }

        response.setSeen(isPrivateRead);
        if (isDeleted) response.setIsDeleted(true);

        // ✅ Redis publish
        redisPublisher.publish(response);
        log.info("🚀 Message sent from consumer to Redis. ID: {}", incomingMessage.getId());

        // Broadcast overviews
        chatMessageService.broadcastChatOverview(incomingMessage.getSender());
        if ("PRIVATE".equalsIgnoreCase(incomingMessage.getType()) && incomingMessage.getReceiver() != null) {
            chatMessageService.broadcastChatOverview(incomingMessage.getReceiver());
        } else if ("TEAM".equalsIgnoreCase(incomingMessage.getType()) && incomingMessage.getGroupId() != null) {
            chatMessageService.broadcastGroupChatOverview(incomingMessage.getGroupId());
        }
    }
}
