package com.app.chat_service.kakfa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    
    @Autowired
    ChatMessageRepository chatMessageRepository;
    
    public void send(ChatMessage message) {
        String topic = switch (message.getType().toUpperCase()) {
            case "TEAM" -> "team-" + message.getGroupId();
            case "DEPARTMENT" -> "department-" + message.getGroupId();
            case "PRIVATE" -> "private-" + message.getReceiver();
            default -> throw new IllegalArgumentException("Invalid chat type");
        };
        
        kafkaTemplate.send(topic, message);
        System.out.println("Message sent to this topic"+topic);
    }
}
