package com.app.chat_service.controller;

import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatKafkaProducer chatKafkaProducer;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sender") String sender,
            @RequestParam(value = "receiver", required = false) String receiver,
            @RequestParam(value = "groupId", required = false) String groupId,
            @RequestParam("type") String type) {

        try {
            // Save message to DB
            ChatMessage message = new ChatMessage();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setGroupId(groupId);
            message.setType(type);
            message.setTimestamp(LocalDateTime.now());
            message.setFileName(file.getOriginalFilename());
            message.setFileType(file.getContentType());
            message.setFileData(file.getBytes());
            message.setContent("File: " + file.getOriginalFilename());

            chatMessageRepository.save(message);

            // Send to Kafka   
            chatKafkaProducer.send(message);

            // Send to WebSocket
            if (groupId != null && !groupId.isBlank()) {
                messagingTemplate.convertAndSend("/topic/group/" + groupId, message);
            } else if (receiver != null && !receiver.isBlank()) {
                messagingTemplate.convertAndSendToUser(receiver, "/queue/private", message);
            }

            // Return download URL
            Map<String, String> response = new HashMap<>();
            response.put("url", "http://localhost:8082/api/chat/file/" + message.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to upload file"));
        }
    }

    @GetMapping("/chat/file/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
        ChatMessage msg = chatMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + msg.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(msg.getFileType()))
                .body(msg.getFileData());
    }
}