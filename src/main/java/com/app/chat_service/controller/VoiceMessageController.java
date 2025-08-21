package com.app.chat_service.controller;

import com.app.chat_service.dto.VoiceMessageRequest;
import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@Slf4j
@RequiredArgsConstructor
public class VoiceMessageController {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatKafkaProducer chatKafkaProducer;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVoiceMessage(@RequestBody VoiceMessageRequest voiceRequest) {
        try {
            if (voiceRequest.getFileData() == null || voiceRequest.getFileData().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File data is empty."));
            }

            // Strip prefix if present
            String base64Data = voiceRequest.getFileData().contains(",")
                    ? voiceRequest.getFileData().substring(voiceRequest.getFileData().indexOf(",") + 1)
                    : voiceRequest.getFileData();

            byte[] audioBytes = Base64.getDecoder().decode(base64Data);

            ChatMessage message = new ChatMessage();
            message.setSender(voiceRequest.getSender());
            message.setReceiver(voiceRequest.getReceiver());
            message.setGroupId(voiceRequest.getGroupId());
            message.setType(voiceRequest.getType().toUpperCase());
            message.setTimestamp(LocalDateTime.now());
            message.setClientId(voiceRequest.getClientId());
            message.setFileName(voiceRequest.getFileName());
            message.setFileType(voiceRequest.getFileType());
            message.setFileData(audioBytes);
            message.setFileSize((long) audioBytes.length);
            message.setContent(voiceRequest.getFileName());
            message.setRead(false);

            ChatMessage savedMessage = chatMessageRepository.save(message);

            log.info("✅ Voice message saved. ID: {}, FileName: {}, Type: {}, Size: {} bytes",
                    savedMessage.getId(),
                    savedMessage.getFileName(),
                    savedMessage.getFileType(),
                    savedMessage.getFileSize()
            );

            chatKafkaProducer.send(savedMessage);

            log.info("📤 Voice message sent to producer. ID: {}, FileName: {}, Type: {}, Size: {} bytes",
                    savedMessage.getId(),
                    savedMessage.getFileName(),
                    savedMessage.getFileType(),
                    savedMessage.getFileSize()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Voice message uploaded successfully",
                    "messageId", savedMessage.getId(),
                    "fileName", savedMessage.getFileName(),
                    "fileSize", savedMessage.getFileSize()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload voice message"));
        }
    }
}
