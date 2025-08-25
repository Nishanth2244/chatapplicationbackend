package com.app.chat_service.redis;

import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** The RedisSubscriber will handle the actual delivery to WebSocket clients.**/


@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ChatPresenceTracker chatTracker;
    private final TeamService teamService;

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

        boolean isWindowOpen = chatTracker.isChatWindowOpen(targetUser, senderUser);
        if (isWindowOpen) {
            messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", chatMessage);
        }

        messagingTemplate.convertAndSendToUser(senderUser, "/queue/private-ack", chatMessage);
    }

    private void handleTeamMessage(ChatMessageResponse chatMessage) {
        String teamId = chatMessage.getGroupId();
        List<String> members = teamService.getEmployeeIdsByTeamId(teamId);

        if (members == null || members.isEmpty()) {
            return;
        }

        messagingTemplate.convertAndSend("/topic/team-" + teamId, chatMessage);
        messagingTemplate.convertAndSendToUser(chatMessage.getSender(), "/queue/group-ack", chatMessage);
    }
}
