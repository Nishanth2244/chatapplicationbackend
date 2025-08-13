package com.app.chat_service.redis;

import com.app.chat_service.dto.ChatMessageResponse; // Use DTO so clientId is preserved
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ChatPresenceTracker chatTracker;
    private final TeamService teamService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            // ✅ Deserialize into DTO containing clientId
            ChatMessageResponse chatMessage = objectMapper.readValue(body, ChatMessageResponse.class);

            if ("PRIVATE".equalsIgnoreCase(chatMessage.getType())) {
                handlePrivateMessage(chatMessage);
            } else if ("TEAM".equalsIgnoreCase(chatMessage.getType())) {
                handleTeamMessage(chatMessage);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Handle private chat messages with ACK support */
    private void handlePrivateMessage(ChatMessageResponse chatMessage) {
        String targetUser = chatMessage.getReceiver();
        String senderUser = chatMessage.getSender();

        // Send message to receiver if their chat window is open
        if (chatTracker.isChatWindowOpen(targetUser, senderUser)) {
            messagingTemplate.convertAndSendToUser(
                targetUser, "/queue/private", chatMessage
            );
            System.out.println("📩 Message sent to receiver " + targetUser);
        }

        // Always send ACK back to sender (includes clientId for tracking)
        messagingTemplate.convertAndSendToUser(
            senderUser, "/queue/private-ack", chatMessage
        );
        System.out.println("✅ ACK sent back to sender " + senderUser);
    }

    /** Handle team chat messages with correct ACK */
    private void handleTeamMessage(ChatMessageResponse chatMessage) {
        String teamId = chatMessage.getGroupId();
        List<String> members = teamService.getEmployeeIdsByTeamId(teamId);

        if (members == null || members.isEmpty()) {
            System.err.println("⚠️ No team members found for teamId: " + teamId);
            return;
        }

        // Broadcast to team topic
        messagingTemplate.convertAndSend("/topic/team-" + teamId, chatMessage);
        System.out.println("📢 Chat broadcasted to team topic: " + teamId);

        // Send group ACK back to sender
        messagingTemplate.convertAndSendToUser(
            chatMessage.getSender(), "/queue/group-ack", chatMessage
        );
        System.out.println("✅ Group ACK sent back to sender " + chatMessage.getSender());
    }
}
