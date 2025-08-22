package com.app.chat_service.redis;

import com.app.chat_service.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisherService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;

    public void publish(Object message) {
        // Publish to Redis
        redisTemplate.convertAndSend(topic.getTopic(), message);
        log.info("✅ Message sent to the Redis Subscriber,, to topic [{}]", topic.getTopic());

        // Also broadcast via WebSocket if it's a chat message
        if (message instanceof ChatMessageResponse response) {
            String type = response.getType().toUpperCase();

            switch (type) {
                case "PRIVATE":
                    messagingTemplate.convertAndSendToUser(
                            response.getReceiver(), "/queue/private", response);
                    log.info("📩 Private message delivered | Sender: {} | Receiver: {} | MessageId: {}",
                            response.getSender(), response.getReceiver(), response.getId());
                    break;

                case "TEAM":
                    messagingTemplate.convertAndSend(
                            "/topic/team-" + response.getGroupId(), response);
                    log.info("👥 Team message delivered | TeamId: {} | Sender: {} | MessageId: {}",
                            response.getGroupId(), response.getSender(), response.getId());
                    break;

                case "DEPARTMENT":
                    messagingTemplate.convertAndSend(
                            "/topic/department-" + response.getGroupId(), response);
                    log.info("🏢 Department message delivered | DepartmentId: {} | Sender: {} | MessageId: {}",
                            response.getGroupId(), response.getSender(), response.getId());
                    break;

                case "DELETED":
                    if (response.getGroupId() != null) {
                        // Group chat deleted message
                        messagingTemplate.convertAndSend(
                                "/topic/team-" + response.getGroupId(), response);
                        log.info("🗑️ Deleted message broadcasted | GroupId: {} | MessageId: {}",
                                response.getGroupId(), response.getId());
                    } else if (response.getReceiver() != null) {
                        // Private deleted message
                        messagingTemplate.convertAndSendToUser(
                                response.getReceiver(), "/queue/private", response);
                        messagingTemplate.convertAndSendToUser(
                                response.getSender(), "/queue/private", response);
                        log.info("🗑️ Deleted private message broadcasted | Sender: {} | Receiver: {} | MessageId: {}",
                                response.getSender(), response.getReceiver(), response.getId());
                    }
                    break;

                default:
                    log.warn("⚠️ Unknown message type [{}] for MessageId: {}", type, response.getId());
            }
        }
    }
}
