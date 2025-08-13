package com.app.chat_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "message_action")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long messageId; // Reference to ChatMessage.id

    @Column(nullable = false)
    private String userId;  // Who performed the action

    @Column(nullable = false)
    private String actionType; // DELETE_ME or DELETE_ALL

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
