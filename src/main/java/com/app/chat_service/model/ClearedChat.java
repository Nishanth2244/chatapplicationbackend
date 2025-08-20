package com.app.chat_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cleared_chat")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearedChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // user who cleared chat

    @Column(nullable = false)
    private String chatId; // groupId or peerId

    @Column(nullable = false)
    private LocalDateTime clearedAt;

    @PrePersist
    protected void onCreate() {
        if (clearedAt == null) {
            clearedAt = LocalDateTime.now();
        }
    }
}
