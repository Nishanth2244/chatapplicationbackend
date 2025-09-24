package com.app.chat_service.feignclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.app.chat_service.dto.EmployeeDTO;
import com.app.chat_service.dto.TeamResponse;

@FeignClient(name = "employee-service", url = "http://hrms.anasolconsultancyservices.com")
public interface EmployeeClient {

    @GetMapping("/api/employee/{id}")
    ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable("id") String id);

    @GetMapping("/api/employee/team/employee/{teamId}")
    ResponseEntity<List<TeamResponse>> getTeamById(@PathVariable("teamId") String teamId);

    @GetMapping("/api/employee/team/{employeeId}")
    ResponseEntity<List<TeamResponse>> getTeamAllEmployees(@PathVariable("employeeId") String employeeId); 
    
    @GetMapping("/api/employee/{employeeId}/image")
    String getEmployeeImage(@PathVariable("employeeId") String employeeId);

}
