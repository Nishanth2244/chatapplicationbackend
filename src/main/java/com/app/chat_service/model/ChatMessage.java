package com.app.chat_service.model;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class ChatMessage {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false)
    private String sender;
 
    @Column
    private String receiver;
 
    @Column(name = "group_id")
    private String groupId;
 
    @Column(nullable = false)
    private String type;
 
    @Column(columnDefinition = "TEXT")
    private String content;
 
    @Column
    private LocalDateTime timestamp;
 
    @Column(name = "file_name")
    private String fileName;
 
    @Column(name = "file_type")
    private String fileType;
 
    @Column(name = "file_size")
    private Long fileSize;
 
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data")
    @JsonIgnore // To avoid sending large byte array in every response
    private byte[] fileData;
    
    @Column(name = "duration")
    private Integer duration;
 
    @Column(name = "is_read")
    private boolean read;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    @JsonIgnore
    private ChatMessage replyToMessage;
 
    @Column(name = "reply_preview", columnDefinition = "TEXT")
    private String replyPreviewContent;
 
    @Column(name = "is_forwarded")
    private Boolean forwarded;
 
    @Column(name = "forwarded_from")
    private String forwardedFrom;
 
    @Column(name = "is_deleted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isDeleted = false;
    
    @Column(name = "is_edited", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isEdited = false;
 
    @Transient
    private boolean group;
 
    @Transient
    private String clientId;
 
    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }
 
    public boolean isGroup() {
        return group;
    }
 
    public void setGroup(boolean group) {
        this.group = group;
    }
 
    @Column(name = "is_pinned")
    private Boolean pinned = false;
 
    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;
}  