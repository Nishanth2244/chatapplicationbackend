package com.app.chat_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.model.ChatMessage;
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

    @Autowired
    DepartmentByIdService departmentByIdService;

    @Autowired
    EmployeeByTeamId employeeByTeamId;

    @Autowired
    AllEmployees allEmployees;

    @Autowired
    TeamService teamService;

    @Autowired
    ChatService chatService;
    
  @Autowired
  ChatMessageService chatMessageService;
  
  @Autowired
  EmployeeClient employeeClient;
  
  private final ChatMessageOverviewService chatMessageOverviewService;

  @GetMapping("/chat/{empId}/{chatId}")
  public ResponseEntity<List<ChatMessageOverviewDTO>> getChatMessages(
          @PathVariable String empId,
          @PathVariable String chatId) {

      List<ChatMessageOverviewDTO> messages = chatMessageOverviewService.getChatMessages(empId, chatId);
      return ResponseEntity.ok(messages);
  }

  @GetMapping("/overview/{employeeId}")
  public ResponseEntity<List<Map<String, Object>>> getChatOverview(@PathVariable String employeeId) {
      List<Map<String, Object>> chatOverview = chatMessageService.getChattedEmployeesInSameTeam(employeeId);
      return ResponseEntity.ok(chatOverview);
  }


    @PostMapping("/send")
    public String sendChat(@RequestBody ChatMessageRequest request) {
        return chatService.sendMessage(request);
    }

    @GetMapping("/team/{teamId}")
    public List<ChatMessageResponse> getTeamMessages(@PathVariable String teamId) {
        List<ChatMessage> messages = chatService.getTeamMessages(teamId);
        return messages.stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/department/{deptId}")
    public List<ChatMessageResponse> getDepartmentMessages(@PathVariable String deptId) {
        List<ChatMessage> messages = chatService.getDepartmentMessages(deptId);
        return messages.stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("chat/history/private")
    public ResponseEntity<List<ChatMessageResponse>> getPrivateChatHistory(
            @RequestParam String sender,
            @RequestParam String receiver) {
        List<ChatMessage> messages = chatService.getPrivateChatHistory(sender, receiver);
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }

    @GetMapping("chat/history/group")
    public ResponseEntity<List<ChatMessageResponse>> getGroupChatHistory(@RequestParam String groupId) {
        List<ChatMessage> messages = chatService.getGroupChatHistory(groupId);
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }

    @GetMapping("/all/departments")
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        return allDeptService.getAllDepartments();
    }

    @GetMapping("/department/{departmentId}/employees")
    public ResponseEntity<EmployeeDepartmentDTO> getEmployeesByDepartmentId(@PathVariable String departmentId) {
        return departmentByIdService.getEmployeesByDeptId(departmentId);
    }

    @GetMapping("/employee/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable("id") String id) {
        return allEmployees.getEmployeeById(id);
    }

    @GetMapping("/team/employee/{teamId}")
    public ResponseEntity<List<TeamResponse>> getTeamById(@PathVariable String teamId) {
        return teamService.getTeamById(teamId);
    }
    

    // Helper method to convert ChatMessage to DTO response
    private ChatMessageResponse toResponse(ChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getSender(),
                msg.getReceiver(),
                msg.getGroupId(),
                msg.getContent(),
                msg.getFileName(),
                msg.getFileType(),
                msg.getType(),
                msg.getTimestamp()
        );
    }
    
    
    @GetMapping("/employee/team/{employeeId}")
    public ResponseEntity<List<TeamResponse>> getAllEmployees(@PathVariable String employeeId) {
        return teamService.ByEmpId(employeeId);
}
}