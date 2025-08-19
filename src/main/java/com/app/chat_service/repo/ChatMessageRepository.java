package com.app.chat_service.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.app.chat_service.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByGroupIdAndType(String groupId, String type);
    List<ChatMessage> findByGroupId(String groupId);
    List<ChatMessage> findBySenderAndReceiverOrReceiverAndSender(String sender, String receiver, String sender2, String receiver2);
    List<ChatMessage> findByFileDataIsNotNull();
    List<ChatMessage> findByGroupIdAndFileDataIsNotNull(String groupId);
    List<ChatMessage> findByReceiverAndFileDataIsNotNull(String receiver);
    List<ChatMessage> findBySenderAndFileDataIsNotNull(String sender);
    List<ChatMessage> findBySenderOrReceiver(String sender, String receiver);
    List<ChatMessage> findBySender(String sender);
    List<ChatMessage> findByReceiver(String receiver);

    // ========================== UNREAD MESSAGE FIX ==========================
    /**
     * Finds all messages sent by a specific sender to a specific receiver
     * that have not yet been marked as read (where the 'read' field is false).
     */
    List<ChatMessage> findBySenderAndReceiverAndReadIsFalse(String sender, String receiver);
    // ========================================================================

    @Query("SELECT m FROM ChatMessage m " +
           "WHERE ((m.sender = :empId AND m.receiver = :chatId) OR (m.sender = :chatId AND m.receiver = :empId)) " +
           "AND m.type = 'PRIVATE' ORDER BY m.timestamp ASC")
    List<ChatMessage> findPrivateChatMessages(@Param("empId") String empId,
                                              @Param("chatId") String chatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.groupId = :teamId AND m.type = 'TEAM' ORDER BY m.timestamp ASC")
    List<ChatMessage> findTeamChatMessages(@Param("teamId") String teamId);

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.groupId = :teamId AND m.type = 'TEAM'
              AND (
                    m.sender = :empId
                    OR (m.receiver IS NOT NULL AND m.receiver = :empId)
                    OR (m.content IS NOT NULL AND m.content LIKE CONCAT('%@', :empId, '%'))
              )
            ORDER BY m.timestamp ASC
            """)
    List<ChatMessage> findTeamChatMessagesForEmployee(@Param("teamId") String teamId,
                                                      @Param("empId") String empId);

    // ========================== FIXED UNREAD COUNTS =========================
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.sender = :senderId " +
           "AND m.receiver = :receiverId " +
           "AND m.read = FALSE " +
           "AND m.type = 'PRIVATE'")
    long countUnreadPrivateMessages(@Param("senderId") String senderId,
                                    @Param("receiverId") String receiverId);

    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.groupId = :groupId " +
           "AND m.type = 'TEAM' " +
           "AND m.sender <> :employeeId " +
           "AND m.read = FALSE")
    long countUnreadGroupMessages(@Param("groupId") String groupId,
                                  @Param("employeeId") String employeeId);
    // ========================================================================
    
    Optional<ChatMessage> findTopByGroupIdAndPinnedIsTrueOrderByPinnedAtDesc(String groupId);
    
    Optional<ChatMessage> findTopBySenderInAndReceiverInAndPinnedIsTrueOrderByPinnedAtDesc(List<String> senders, List<String> receivers);
 
    @Modifying
    @Query("UPDATE ChatMessage m SET m.pinned = false, m.pinnedAt = null WHERE m.pinned = true AND ((m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) OR m.groupId = :chatId)")
    void unpinAllMessagesInChat(@Param("chatId") String chatId, @Param("user1") String user1, @Param("user2") String user2);
 


}
