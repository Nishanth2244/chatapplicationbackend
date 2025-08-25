package com.app.chat_service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisherService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;

    /**
     * Publishes the message to the Redis topic.
     */
    public void publish(Object message) {
        // The ONLY responsibility of the publisher is to send the message to Redis.
        redisTemplate.convertAndSend(topic.getTopic(), message);
        log.info("✅ Message published to Redis topic [{}]", topic.getTopic());
    }
}