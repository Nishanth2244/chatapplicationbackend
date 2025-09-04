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

@FeignClient(name = "employee-client", url = "http://hrms.anasolconsultancyservices.com/api/employee/")
public interface EmployeeClient {

    @GetMapping("all/departments")
    List<DepartmentDTO> getAllDepartments();

    @GetMapping("department/{departmentId}/employees")
    ResponseEntity<EmployeeDepartmentDTO> getEmployeesByDepartmentId(@PathVariable("departmentId") String departmentId);

    @GetMapping("{id}")
    ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable("id") String id);

    @GetMapping("team/{teamId}")
    ResponseEntity<List<TeamResponse>> getTeamById(@PathVariable("teamId") String teamId);

    @GetMapping("team/{employeeId}")
    ResponseEntity<List<TeamResponse>> getTeamAllEmployees(@PathVariable("employeeId") String employeeId); 

}
