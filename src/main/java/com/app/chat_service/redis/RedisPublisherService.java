package com.app.chat_service.redis;
 
import com.app.chat_service.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
 
@Service
@RequiredArgsConstructor
public class RedisPublisherService {
 
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;
 
    public void publish(Object message) {
        redisTemplate.convertAndSend(topic.getTopic(), message);
        System.out.println("✅ Published message to Redis topic: " + topic.getTopic());
 
        if (message instanceof ChatMessageResponse response) {
            switch (response.getType().toUpperCase()) {
                case "PRIVATE":
                    messagingTemplate.convertAndSendToUser(
                            response.getReceiver(), "/queue/private", response);
                    break;
                case "TEAM":
                    messagingTemplate.convertAndSend(
                            "/topic/team-" + response.getGroupId(), response);
                    break;
                case "DEPARTMENT":
                    messagingTemplate.convertAndSend(
                            "/topic/department-" + response.getGroupId(), response);
                    break;
                // ======================= BUG FIX START =======================
                case "DELETED":
                    // Handle broadcasting the deleted message status
                    if (response.getGroupId() != null) {
                        // This is a group chat message (TEAM or DEPARTMENT)
                        messagingTemplate.convertAndSend(
                                "/topic/team-" + response.getGroupId(), response);
                    } else if (response.getReceiver() != null) {
                        // This is a private message. Notify both the original sender and receiver
                        // so all of their connected sessions get the update.
                        messagingTemplate.convertAndSendToUser(
                                response.getReceiver(), "/queue/private", response);
                        messagingTemplate.convertAndSendToUser(
                                response.getSender(), "/queue/private", response);
                    }
                    break;
                // ======================= BUG FIX END =======================
            }
        }
    }
}