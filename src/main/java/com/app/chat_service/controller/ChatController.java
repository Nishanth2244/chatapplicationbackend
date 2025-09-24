package com.app.chat_service.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.chat_service.dto.ChatMessageOverviewDTO;
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.dto.EmployeeDTO;
import com.app.chat_service.dto.GroupChatDetailsResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.employee_details;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.service.AllEmployees;
import com.app.chat_service.service.ChatMessageOverviewService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatService;
import com.app.chat_service.service.EmployeeByTeamId;
import com.app.chat_service.service.EmployeeDetailsService;
import com.app.chat_service.service.TeamService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final EmployeeByTeamId employeeByTeamId;
    private final AllEmployees allEmployees;
    private final TeamService teamService;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final EmployeeClient employeeClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageOverviewService chatMessageOverviewService;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeDetailsService employeeDetailsService;   
    
    
    /** Fetch messages between employee and chatId (could be private or group) */
    @GetMapping("/{empId}/{chatId}")
    public ResponseEntity<List<ChatMessageOverviewDTO>> getChatMessages(
            @PathVariable("empId") String empId,
            @PathVariable("chatId") String chatId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size) {
    	
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        List<ChatMessageOverviewDTO> messages = chatMessageOverviewService.getChatMessages(empId, chatId, pageable);
        Collections.reverse(messages);
        return ResponseEntity.ok(messages);
    }

    /** Sidebar Overview (Private + Group Chats) */
    @GetMapping("/overview/{employeeId}")
    public ResponseEntity<List<Map<String, Object>>> getChatOverview(
            @PathVariable("employeeId") String employeeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        List<Map<String, Object>> chatOverview = chatMessageService.getChattedEmployeesInSameTeam(employeeId, page, size);
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

    /** Private chat history */
    @GetMapping("/history/private")
    public ResponseEntity<List<ChatMessageResponse>> getPrivateChatHistory(
            @RequestParam("sender") String sender,
            @RequestParam("receiver") String receiver) {
        List<ChatMessage> messages = chatService.getPrivateChatHistory(sender, receiver);
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }

    /** Group chat history */
//    @GetMapping("/history/group")
//    public ResponseEntity<List<ChatMessageResponse>> getGroupChatHistory(@RequestParam("groupId") String groupId) {
//        List<ChatMessage> messages = chatService.getGroupChatHistory(groupId);
//        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
//    }


    /** Get Employee details */
    @GetMapping("/employee/{id}")
    public employee_details getEmployeeById(@PathVariable("id") String id) {
        return allEmployees.getEmployeeById(id);
    }

    /** Team details */
    @GetMapping("/team/employee/{teamId}")
    public List<TeamResponse> getTeamById(@PathVariable("teamId") String teamId) {
    	List<TeamResponse> memberString=teamService.getGroupMembers(teamId);
    	log.info("members fetched from the teamservice {}", memberString);
    	return memberString;
    	}

    /** Teams for an Employee */
    @GetMapping("/employee/team/{employeeId}")
    public ResponseEntity<List<TeamResponse>> getTeamsByEmpId(@PathVariable("employeeId") String employeeId) {
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
                msg.getClientId(),
                msg.getDuration()
                );
    }
    
    
    @PostMapping("/internal/notify-team-update")
    public ResponseEntity<Void> notifyTeamUpdate(@RequestParam String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Received team update notification for teamId: {}. Broadcasting overview.", teamId);
        // This service call will update the sidebar for all team members
        chatMessageService.broadcastGroupChatOverview(teamId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/employee/add")
    public void addEmployee(@RequestBody List<employee_details> employee_details_all) {
    	 for(employee_details i : employee_details_all ) {
    		 employeeDetailsService.addEmployee(i);
    	 }
    	 
    }
    
    @PutMapping("/employee/update/{employeeId}")
    public employee_details empupdate(
    		@PathVariable String employeeId,
    		@RequestBody employee_details updateDetails) {
    	
    	employee_details emp=employeeDetailsService.updateEmployee(employeeId, updateDetails);
    	return emp;
    			}
    
    @DeleteMapping("/employee/delete/{employeeId}")
    public ResponseEntity<String> deleteEmployee(@PathVariable String employeeId){
    	employeeDetailsService.deleteById(employeeId);
    	return ResponseEntity.ok("Employee Succesfully Deleted");   	
    }
    
    
    
    
//    method for clearing the Redis Cache
    @PostMapping("/internal/cache/evict-team")
    public ResponseEntity<Void> evictTeamCache(@RequestParam String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Received request to evict cache for teamId: {}", teamId);
        teamService.evictTeamCaches(teamId);
        return ResponseEntity.ok().build();
    }
    
    
    
//    NOT YET  CALLED BY EMPLOYEE SERVICE
//    Method for cleaning the teams of an employee belongs
    @PostMapping("/internal/cache/evict-employee-teams")
    public ResponseEntity<Void> evictEmployeeTeamsCache(@RequestParam String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Received request to evict teams cache of an employeeId: {}", employeeId);
        teamService.evictEmployeeTeamsCache(employeeId);
        return ResponseEntity.ok().build();
    }
     
}
    
   