package com.app.chat_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.chat_service.dto.ChatMessageOverviewDTO;
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.dto.DepartmentDTO;
import com.app.chat_service.dto.EmployeeDTO;
import com.app.chat_service.dto.EmployeeDepartmentDTO;
import com.app.chat_service.dto.GroupChatDetailsResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.dto.TypingStatusDTO;
import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.service.AllDeptService;
import com.app.chat_service.service.AllEmployees;
import com.app.chat_service.service.ChatMessageOverviewService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatService;
import com.app.chat_service.service.DepartmentByIdService;
import com.app.chat_service.service.EmployeeByTeamId;
import com.app.chat_service.service.TeamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final AllDeptService allDeptService;
    private final DepartmentByIdService departmentByIdService;
    private final EmployeeByTeamId employeeByTeamId;
    private final AllEmployees allEmployees;
    private final TeamService teamService;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final EmployeeClient employeeClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageOverviewService chatMessageOverviewService;
    private final ChatMessageRepository chatMessageRepository;
    /** Fetch messages between employee and chatId (could be private or group) */
    @GetMapping("/chat/{empId}/{chatId}")
    public ResponseEntity<List<ChatMessageOverviewDTO>> getChatMessages(
            @PathVariable("empId") String empId,
            @PathVariable("chatId") String chatId) {
        List<ChatMessageOverviewDTO> messages = chatMessageOverviewService.getChatMessages(empId, chatId);
        return ResponseEntity.ok(messages);
    }

    /** Sidebar Overview (Private + Group Chats) */
    @GetMapping("/overview/{employeeId}")
    public ResponseEntity<List<Map<String, Object>>> getChatOverview(@PathVariable("employeeId") String employeeId) {
        List<Map<String, Object>> chatOverview = chatMessageService.getChattedEmployeesInSameTeam(employeeId);
        return ResponseEntity.ok(chatOverview);
    }

    /** Send message (REST API) */
    @PostMapping("/send")
    public String sendChat(@RequestBody ChatMessageRequest request) {
        return chatService.sendMessage(request);
    }

    /** Fetch all messages of a Team */
    @GetMapping("/team/{teamId}")
    public List<ChatMessageResponse> getTeamMessages(@PathVariable String teamId) {
        List<ChatMessage> messages = chatService.getTeamMessages(teamId);
        return messages.stream().map(this::toResponse).toList();
    }

    /** Fetch all messages of a Department */
    @GetMapping("/department/{deptId}")
    public List<ChatMessageResponse> getDepartmentMessages(@PathVariable String deptId) {
        List<ChatMessage> messages = chatService.getDepartmentMessages(deptId);
        return messages.stream().map(this::toResponse).toList();
    }

    /** Private chat history */
    @GetMapping("chat/history/private")
    public ResponseEntity<List<ChatMessageResponse>> getPrivateChatHistory(
            @RequestParam("sender") String sender,
            @RequestParam("receiver") String receiver) {
        List<ChatMessage> messages = chatService.getPrivateChatHistory(sender, receiver);
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }

    /** Group chat history */
    @GetMapping("chat/history/group")
    public ResponseEntity<List<ChatMessageResponse>> getGroupChatHistory(@RequestParam("groupId") String groupId) {
        List<ChatMessage> messages = chatService.getGroupChatHistory(groupId);
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }

    /** Departments List */
    @GetMapping("/all/departments")
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        return allDeptService.getAllDepartments();
    }

    /** Employees by Department */
    @GetMapping("/department/{departmentId}/employees")
    public ResponseEntity<EmployeeDepartmentDTO> getEmployeesByDepartmentId(
            @PathVariable("departmentId") String departmentId) {
        return departmentByIdService.getEmployeesByDeptId(departmentId);
    }

    /** Get Employee details */
    @GetMapping("/employee/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable("id") String id) {
        return allEmployees.getEmployeeById(id);
    }

    /** Team details */
    @GetMapping("/team/employee/{teamId}")
    public TeamResponse getTeamById(@PathVariable("teamId") String teamId) {
        return teamService.getGroupById(teamId);
    }

    /** Group Chat details (like members, name, etc.) */
    @GetMapping("/group/chat/details/{groupId}")
    public ResponseEntity<GroupChatDetailsResponse> getGroupChatDetails(@PathVariable("groupId") String groupId) {
        GroupChatDetailsResponse response = teamService.getGroupChatDetails(groupId);
        return ResponseEntity.ok(response);
    }

    /** Teams for an Employee */
    @GetMapping("/employee/team/{employeeId}")
    public ResponseEntity<List<TeamResponse>> getAllEmployees(@PathVariable("employeeId") String employeeId) {
        return teamService.ByEmpId(employeeId);
    }


    /** Helper to map ChatMessage -> ChatMessageResponse */
    private ChatMessageResponse toResponse(ChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getSender(),
                msg.getReceiver(),
                msg.getGroupId(),
                msg.getContent(),
                msg.getFileName(),
                msg.getFileType(),
                msg.getFileSize(),   // ✅ Pass fileSize in correct position
                msg.getType(),
                msg.getTimestamp(),
                msg.getFileData(),
                msg.getClientId()
        );
    }
}