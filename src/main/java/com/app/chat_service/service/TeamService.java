package com.app.chat_service.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.chat_service.dto.GroupChatDetailsResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.repo.ChatMessageRepository;

@Service
public class TeamService {

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /** Check if a given team exists */
    public boolean existsByTeamId(String teamId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Get all teams where an employee belongs */
    public List<TeamResponse> getTeamsByEmployeeId(String employeeId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamsByEmployeeId(employeeId);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Get all employees in all teams of a given employee */
    public List<TeamResponse> getEmployeesInAllTeamsOf(String employeeId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamsByEmployeeId(employeeId);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Get team details by teamId */
    public TeamResponse getGroupById(String teamId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                return response.getBody().get(0);
            }
        } catch (Exception e) {
            // log error
        }
        return null;
    }

    /** Get all members of a group */
    public List<TeamResponse> getGroupMembers(String teamId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Get just the employee IDs of a given team */
    @Cacheable(value = "teamMembers", key = "#teamId")
    public List<String> getEmployeeIdsByTeamId(String teamId) {
        List<TeamResponse> teams = getGroupMembers(teamId);
        if (teams.isEmpty()) {
            return Collections.emptyList();
        }
        return teams.get(0).getEmployees()
                .stream()
                .map(emp -> emp.getEmployeeId())
                .collect(Collectors.toList());
    }

    /** Placeholder for group chat details (to be implemented) */
    public GroupChatDetailsResponse getGroupChatDetails(String groupId) {
        return null; // TODO implement if needed
    }
    
    public ResponseEntity<List<TeamResponse>> ByEmpId(String employeeId) {
        try {
            // Call Feign client directly
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamsByEmployeeId(employeeId);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody() != null ? response.getBody() : Collections.emptyList());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyList());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

}