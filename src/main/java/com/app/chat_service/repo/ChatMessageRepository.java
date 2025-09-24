package com.app.chat_service.repo;

import com.app.chat_service.model.ChatMessage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByGroupIdAndType(String groupId, String type);
    List<ChatMessage> findByGroupId(String groupId);
    List<ChatMessage> findBySenderAndReceiverOrReceiverAndSender(
            String sender, String receiver, String sender2, String receiver2);

    List<ChatMessage> findByFileDataIsNotNull();
    List<ChatMessage> findByGroupIdAndFileDataIsNotNull(String groupId);
    List<ChatMessage> findByReceiverAndFileDataIsNotNull(String receiver);
    List<ChatMessage> findBySenderAndFileDataIsNotNull(String sender);
    List<ChatMessage> findBySenderOrReceiver(String sender, String receiver);
    List<ChatMessage> findBySender(String sender);
    List<ChatMessage> findByReceiver(String receiver);

    // ================== PRIVATE CHAT UNREAD ==================

    /**
     * Count all unread private messages sent by chatPartner to employee.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.sender = :chatPartnerId " +
           "AND m.receiver = :employeeId " +
           "AND m.type = 'PRIVATE' " +
           "AND m.read = FALSE")
    long countUnreadPrivateMessages(@Param("chatPartnerId") String chatPartnerId,
                                    @Param("employeeId") String employeeId);

    /**
     * Find all unread private messages from chatPartner → employee.
     */
    List<ChatMessage> findBySenderAndReceiverAndReadIsFalse(String sender, String receiver);

    // ================== GROUP CHAT UNREAD ==================

    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.groupId = :groupId " +
           "AND m.type = 'TEAM' " +
           "AND m.sender <> :employeeId " +
           "AND m.read = FALSE")
    long countUnreadGroupMessages(@Param("groupId") String groupId,
                                  @Param("employeeId") String employeeId);

    // ================== FETCH CHAT MESSAGES ==================

    @Query("SELECT m FROM ChatMessage m " +
           "WHERE ((m.sender = :empId AND m.receiver = :chatId) OR (m.sender = :chatId AND m.receiver = :empId)) " +
           "AND m.type = 'PRIVATE' ORDER BY m.timestamp ASC")
    List<ChatMessage> findPrivateChatMessages(@Param("empId") String empId,
                                              @Param("chatId") String chatId);

    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.groupId = :teamId AND m.type = 'TEAM' ORDER BY m.timestamp ASC")
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

    // ================== PINNED ==================

    Optional<ChatMessage> findTopByGroupIdAndPinnedIsTrueOrderByPinnedAtDesc(String groupId);
    
    Optional<ChatMessage> findTopBySenderInAndReceiverInAndPinnedIsTrueOrderByPinnedAtDesc(List<String> senders, List<String> receivers);
 
    @Modifying
    @Query("UPDATE ChatMessage m " +
           "SET m.pinned = false, m.pinnedAt = null " +
           "WHERE m.pinned = true AND " +
           "((m.sender = :user1 AND m.receiver = :user2) " +
           "OR (m.sender = :user2 AND m.receiver = :user1) " +
           "OR m.groupId = :chatId)")
    void unpinAllMessagesInChat(@Param("chatId") String chatId,
                                @Param("user1") String user1,
                                @Param("user2") String user2);

    // ================== CLEARED CHAT ==================

    @Query("SELECT m FROM ChatMessage m " +
           "WHERE ((m.sender = :empId AND m.receiver = :chatId) OR (m.sender = :chatId AND m.receiver = :empId)) " +
           "AND m.type = 'PRIVATE' AND m.timestamp > :clearedAt")
    Page<ChatMessage> findPrivateChatMessagesAfter(@Param("empId") String empId,
                                                   @Param("chatId") String chatId,
                                                   @Param("clearedAt") LocalDateTime clearedAt,
                                                   Pageable pageable);

    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.groupId = :teamId AND m.type = 'TEAM' AND m.timestamp > :clearedAt")
    Page<ChatMessage> findTeamChatMessagesAfter(@Param("teamId") String teamId,
                                                @Param("clearedAt") LocalDateTime clearedAt,
                                                Pageable pageable);
    
    
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
    	       "WHERE m.groupId = :groupId AND m.type = 'TEAM' AND m.sender <> :userId " +
    	       "AND m.timestamp > :clearedAt " +
    	       "AND m.id NOT IN (" +
    	       "  SELECT mrs.chatMessage.id FROM MessageReadStatus mrs " +
    	       "  WHERE mrs.userId = :userId AND mrs.chatMessage.groupId = :groupId" +
    	       ")")
    	long countUnreadMessagesForUserInGroup(@Param("userId") String userId,
    	                                       @Param("groupId") String groupId,
    	                                       @Param("clearedAt") LocalDateTime clearedAt);
    
    
    @Query("SELECT DISTINCT m.receiver FROM ChatMessage m WHERE m.sender = :employeeId AND m.receiver IS NOT NULL")
    Set<String> findDistinctReceiversBySender(@Param("employeeId") String employeeId);

    @Query("SELECT DISTINCT m.sender FROM ChatMessage m WHERE m.receiver = :employeeId AND m.sender IS NOT NULL")
    Set<String> findDistinctSendersByReceiver(@Param("employeeId") String employeeId);
    

//    UNREAD MESSAGE COUNT AFTER THE CHAT CLEARED IN PRIVATE
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiver = :userId AND m.sender = :chatPartnerId AND m.read = false AND m.timestamp > :clearedAt AND m.type = 'PRIVATE'")
    long countUnreadPrivateMessages(@Param("userId") String userId, @Param("chatPartnerId") String chatPartnerId, @Param("clearedAt") LocalDateTime clearedAt);

    Optional<ChatMessage> findTopByGroupIdAndTypeAndTimestampAfterOrderByTimestampDesc(
            String groupId,
            String type,
            LocalDateTime timestamp
    );
    
    Optional<ChatMessage> findTopBySenderAndReceiverOrReceiverAndSenderOrderByTimestampDesc(
            String sender,
            String receiver,
            String receiver2,
            String sender2
    );
    
    
    Optional<ChatMessage> findTopByTypeAndSenderInAndReceiverInAndTimestampAfterOrderByTimestampDesc(
    	    String type,
    	    List<String> senders,
    	    List<String> receivers,
    	    LocalDateTime timestamp
    	);
    
}

