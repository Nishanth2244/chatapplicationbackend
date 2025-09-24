package com.app.chat_service.model;
 
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "message_read_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReadStatus {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage chatMessage;
 
    @Column(name = "user_id", nullable = false)
    private String userId;
 
    @Column(name = "read_at")
    private LocalDateTime readAt;
}