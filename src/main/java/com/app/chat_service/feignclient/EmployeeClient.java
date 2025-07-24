package com.app.chat_service.feignclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.app.chat_service.dto.DepartmentDTO;
import com.app.chat_service.dto.EmployeeDTO;
import com.app.chat_service.dto.EmployeeDepartmentDTO;
import com.app.chat_service.dto.TeamResponse;


@FeignClient(name = "employee-client", url = "http://localhost:8080")
public interface EmployeeClient {

    @GetMapping("/api/all/departments")
    List<DepartmentDTO> getAllDepartments();

    @GetMapping("/api/department/{departmentId}/employees")
    ResponseEntity<EmployeeDepartmentDTO> getEmployeesByDepartmentId(@PathVariable String departmentId);

    @GetMapping("/api/employee/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable String id);
    
    @GetMapping("api/team/employee/{teamId}")
    public ResponseEntity<List<TeamResponse>> getTeamById(@PathVariable String teamId);
    
    @GetMapping("api/employee/team/{employeeId}")
    public ResponseEntity<List<TeamResponse>> getAllEmployees(@PathVariable String employeeId);
}

