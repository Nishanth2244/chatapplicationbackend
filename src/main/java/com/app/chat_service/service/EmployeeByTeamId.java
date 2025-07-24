package com.app.chat_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.EmployeeClient;

@Service
public class EmployeeByTeamId {
	
	@Autowired
	EmployeeClient employeeClient;
	
    public ResponseEntity<List<TeamResponse>> getTeamById(@PathVariable String teamId){
    	return employeeClient.getTeamById(teamId);
    }

}
