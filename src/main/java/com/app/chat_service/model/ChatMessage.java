package com.app.chat_service.model;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
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
 
    // Sender & Receiver details
    @Column(nullable = false)
    private String sender;
 
    @Column
    private String receiver;
 
    @Column(name = "group_id")
    private String groupId;
 
    @Column(nullable = false)
    private String type;
 
    // Message content
    @Column(columnDefinition = "TEXT")
    private String content;
 
    @Column
    private LocalDateTime timestamp;
 
    // ======================= BUG FIX START =======================
    // File details
    @Column(name = "file_name")
    private String fileName;
 
    @Column(name = "file_type")
    private String fileType;
   
    @Column(name = "file_size") // <<< 1. FILESIZE FIELD ADD CHEYANDI
    private Long fileSize;
 
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data")
    private byte[] fileData;
    // ======================= BUG FIX END =========================
 
    @Column(name = "is_read")
    private boolean read;
 
    // Reply-related fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    @JsonIgnore
    private ChatMessage replyToMessage;
 
    @Column(name = "reply_preview", columnDefinition = "TEXT")
    private String replyPreviewContent;
 
    // Forwarding-related fields
    @Column(name = "is_forwarded")
    private Boolean forwarded;
 
    @Column(name = "forwarded_from")
    private String forwardedFrom;
 
    // Not persisted, used for runtime purposes
    @Transient
    private boolean group;
 
    // Client tracking ID (not stored in DB)
    @Transient
    private String clientId;
 
 
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
 