package com.app.chat_service.repo;
 
import com.app.chat_service.model.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.util.List;
import java.util.Set;
 
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {
 
    // Find all message IDs that a specific user has read in a specific group
    @Query("SELECT mrs.chatMessage.id FROM MessageReadStatus mrs WHERE mrs.userId = :userId AND mrs.chatMessage.groupId = :groupId")
    Set<Long> findReadMessageIdsByUserIdAndGroupId(@Param("userId") String userId, @Param("groupId") String groupId);
}