package com.app.chat_service.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.chat_service.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // ✅ Existing methods (unchanged)
    List<ChatMessage> findByGroupIdAndType(String groupId, String type);

    List<ChatMessage> findByGroupId(String groupId);

    List<ChatMessage> findBySenderAndReceiverOrReceiverAndSender(String sender, String receiver, String sender2, String receiver2);

    // ✅ Newly added (optional utility) methods

    // Find all messages with files
    List<ChatMessage> findByFileDataIsNotNull();

    // Find group messages that include files
    List<ChatMessage> findByGroupIdAndFileDataIsNotNull(String groupId);

    // Find private messages received by a user that have files
    List<ChatMessage> findByReceiverAndFileDataIsNotNull(String receiver);

    // Find private messages sent by a user that have files
    List<ChatMessage> findBySenderAndFileDataIsNotNull(String sender);
    
    List<ChatMessage> findBySenderOrReceiver(String sender, String receiver);
    
    List<ChatMessage> findBySender(String sender);

    List<ChatMessage> findByReceiver(String receiver);
    
    @Query("SELECT m FROM ChatMessage m WHERE " +
    	       "((m.sender = :empId AND m.receiver = :chatId) OR (m.sender = :chatId AND m.receiver = :empId)) " +
    	       "AND m.type = 'PRIVATE' ORDER BY m.timestamp ASC")
    	List<ChatMessage> findPrivateChatMessages(@Param("empId") String empId,
    	                                          @Param("chatId") String chatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.groupId = :teamId AND m.type = 'TEAM' ORDER BY m.timestamp ASC")
    List<ChatMessage> findTeamChatMessages(@Param("teamId") String teamId);

   

    

   

    

  

}
