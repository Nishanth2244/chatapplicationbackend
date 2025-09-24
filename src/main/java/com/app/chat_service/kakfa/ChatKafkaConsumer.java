//package com.app.chat_service.kakfa;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//import com.app.chat_service.model.ChatMessage;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ChatKafkaConsumer {
//
//    private final KafkaMessageProcessorService messageProcessor;
//
//    @KafkaListener(
//            topics = {"team", "department", "private"},
//            groupId = "chat-group",
//            containerFactory = "chatKafkaListenerContainerFactory"
//    )
//    public void consume(ChatMessage incomingMessage) {
//        // It returns immediately, freeing the thread to poll Kafka for the next message.
//        log.info("📥 Message {} received by consumer. Offloading to async processor.", incomingMessage.getId());
//        messageProcessor.processChatMessage(incomingMessage);
//    }
//}