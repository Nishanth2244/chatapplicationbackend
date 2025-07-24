package com.app.chat_service.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.EmployeeClient;

@Service
public class TeamService {

    @Autowired
    private EmployeeClient employeeClient;

    public boolean existsByTeamId(String teamId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public List<TeamResponse> getTeamsByEmployeeId(String employeeId) {
        ResponseEntity<List<TeamResponse>> response = employeeClient.getAllEmployees(employeeId);
        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }

    // Used by ChatMessageService to fetch all teams with employees for the given employeeId
    public List<TeamResponse> getEmployeesInAllTeamsOf(String employeeId) {
        ResponseEntity<List<TeamResponse>> response = employeeClient.getAllEmployees(employeeId);
        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }

    public ResponseEntity<List<TeamResponse>> getTeamById(String teamId) {
        return employeeClient.getTeamById(teamId);
    }



	    public ResponseEntity<List<TeamResponse>> ByEmpId(@PathVariable String employeeId) {
	    	return employeeClient.getAllEmployees(employeeId);
	    } 
	    
}
