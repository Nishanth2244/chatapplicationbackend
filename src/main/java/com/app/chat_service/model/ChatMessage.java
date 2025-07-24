package com.app.chat_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sender;      // Employee ID or username of sender

    @Column
    private String receiver;    // For private chat only (nullable)

    @Column(name = "group_id")
    private String groupId;     // For group chat (nullable)

    @Column(nullable = false)
    private String type;        // "PRIVATE", "TEAM", "DEPARTMENT", etc.

    @Column(columnDefinition = "TEXT")
    private String content;     // Message content

    @Column
    private LocalDateTime timestamp; // When the message was sent

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    // FIX: Do NOT use columnDefinition here
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(name = "is_read")
    private boolean read;
}
