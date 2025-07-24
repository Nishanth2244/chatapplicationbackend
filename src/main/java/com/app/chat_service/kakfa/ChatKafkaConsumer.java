package com.app.chat_service.kakfa;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatKafkaConsumer {
	
	private final SimpMessagingTemplate messagingTemplate;
	
	
	@Autowired
	ChatMessageRepository chatMessageRepository;
	
	@KafkaListener(
		    topicPattern = "^(team|department|private)-.*$",
		    groupId = "chat-group",
		    containerFactory = "chatKafkaListenerContainerFactory"
		)
		public void consume(ChatMessage message) {
		    System.out.println("Message received: " + message);

		    if (message.getTimestamp() == null) {
		        message.setTimestamp(LocalDateTime.now());
		    }

		    chatMessageRepository.save(message);
		    System.out.println("Message received in topic");
		    
		    
		    
		    String destination = switch (message.getType().toUpperCase()) {
            case "PRIVATE" -> "/queue/private-" + message.getReceiver();
            case "TEAM" -> "/topic/team-" + message.getGroupId();
            case "DEPARTMENT" -> "/topic/department-" + message.getGroupId();
            default -> throw new IllegalArgumentException("Unknown message type");
        };

        messagingTemplate.convertAndSend(destination, message);
		}
	
}