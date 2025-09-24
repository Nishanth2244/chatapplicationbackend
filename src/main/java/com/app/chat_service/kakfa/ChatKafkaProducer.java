//package com.app.chat_service.kakfa;
//
//import java.util.Set;
//
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import com.app.chat_service.model.ChatMessage;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class ChatKafkaProducer {
//
//    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
//    
//    @Async("asyncTaskExecutor")
//    public void send(ChatMessage message) {
//        String type = (message.getType() == null) ? "" : message.getType().toUpperCase();
//
//        if (!Set.of("TEAM", "DEPARTMENT", "PRIVATE").contains(type)) {
//            throw new IllegalArgumentException("Invalid chat type: " + type);
//        }
//
//        String topic = switch (type) {
//            case "TEAM" -> "team";
//            case "DEPARTMENT" -> "department";
//            default -> "private";
//        };
//
//        kafkaTemplate.send(topic, message);
//        System.out.println("📤 Message sent to Kafka topic: " + topic);
//    }
//}