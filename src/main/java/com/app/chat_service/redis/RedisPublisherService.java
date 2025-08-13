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
                case "PRIVATE" ->
                        messagingTemplate.convertAndSendToUser(
                                response.getReceiver(), "/queue/private", response);
                case "TEAM" ->
                        messagingTemplate.convertAndSend(
                                "/topic/team-" + response.getGroupId(), response);
                case "DEPARTMENT" ->
                        messagingTemplate.convertAndSend(
                                "/topic/department-" + response.getGroupId(), response);
            }
        }
    }
}
