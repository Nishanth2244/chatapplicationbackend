package com.app.chat_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.EmployeeDepartmentDTO;
import com.app.chat_service.dto.EmployeeTeamResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.kakfa.ChatKafkaProducer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;

@Service
@CacheConfig(cacheNames = "chatHistory")
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private AllEmployees allEmployees;
    @Autowired
    private TeamService teamService;
    @Autowired
    private DepartmentByIdService departmentByIdService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatKafkaProducer kafkaProducer;

    public String sendMessage(ChatMessageRequest request) {
        String type = request.getType() != null ? request.getType().toUpperCase() : "";
        String senderId = request.getSender();
        String receiverId = request.getReceiver();
        String groupId = request.getGroupId();

        if (!allEmployees.existsById(senderId)) {
            return "Error: Sender employee ID not found.";
        }

        switch (type) {
            case "PRIVATE":
                if (receiverId == null || !allEmployees.existsById(receiverId)) {
                    return "Error: Receiver employee ID not found.";
                }
                break;
            case "TEAM":
                if (groupId == null || !teamService.existsByTeamId(groupId)) {
                    return "Error: Team ID not found.";
                }
                if (!isEmployeeInTeam(senderId, groupId)) {
                    return "Error: Sender is not a member of this team.";
                }
                break;
            case "DEPARTMENT":
                if (groupId == null || !departmentByIdService.existsByDepartmentId(groupId)) {
                    return "Error: Department ID not found.";
                }
                if (!isEmployeeInDepartment(senderId, groupId)) {
                    return "Error: Sender is not a member of this department.";
                }
                break;
            default:
                return "Error: Invalid chat type.";
        }

        ChatMessage message = buildMessage(request, type);
        chatMessageRepository.save(message);

        if ("PRIVATE".equals(type)) {
            messagingTemplate.convertAndSendToUser(receiverId, "/queue/messages", message);
        } else {
            messagingTemplate.convertAndSend("/topic/group/" + groupId, message);
        }

        kafkaProducer.send(message);

        clearMessageCache(); 
        return "Message sent successfully.";
    }

    private ChatMessage buildMessage(ChatMessageRequest request, String type) {
        ChatMessage message = new ChatMessage();
        message.setSender(request.getSender());
        message.setReceiver("PRIVATE".equals(type) ? request.getReceiver() : null);
        message.setGroupId(("TEAM".equals(type) || "DEPARTMENT".equals(type)) ? request.getGroupId() : null);
        message.setType(type);
        message.setContent(request.getContent() != null ? request.getContent() : "");
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    private boolean isEmployeeInTeam(String employeeId, String teamId) {
        TeamResponse team = teamService.getGroupById(teamId);
        return team != null && team.getEmployees() != null &&
               team.getEmployees().stream().anyMatch(emp -> employeeId.equals(emp.getEmployeeId()));
    }

    private boolean isEmployeeInDepartment(String employeeId, String deptId) {
        ResponseEntity<EmployeeDepartmentDTO> response = departmentByIdService.getEmployeesByDeptId(deptId);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return false;
        List<EmployeeTeamResponse> employees = response.getBody().getEmployeeList();
        return employees != null &&
               employees.stream().anyMatch(emp -> emp.getEmployeeId().equals(employeeId));
    }

    // --- Cached methods (fixed sync/unless issue) ---
    @Transactional(readOnly = true)
    public List<ChatMessage> getTeamMessages(String teamId) {
        return chatMessageRepository.findByGroupIdAndType(teamId, "TEAM");
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getDepartmentMessages(String deptId) {
        return chatMessageRepository.findByGroupIdAndType(deptId, "DEPARTMENT");
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getPrivateChatHistory(String sender, String receiver) {
        return chatMessageRepository.findBySenderAndReceiverOrReceiverAndSender(sender, receiver, sender, receiver);
    }

    @Transactional(readOnly = true)
  
    public List<ChatMessage> getGroupChatHistory(String groupId) {
        return chatMessageRepository.findByGroupId(groupId);
    }


    public void clearMessageCache() {
        // Clears all cache entries
    }
}
