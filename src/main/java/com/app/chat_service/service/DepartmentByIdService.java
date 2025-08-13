package com.app.chat_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.chat_service.dto.EmployeeDepartmentDTO;
import com.app.chat_service.dto.EmployeeTeamResponse;
import com.app.chat_service.feignclient.EmployeeClient;

@Service
public class DepartmentByIdService {

    @Autowired
    private EmployeeClient employeeClient;

    public ResponseEntity<EmployeeDepartmentDTO> getEmployeesByDeptId(String departmentId) {
        ResponseEntity<EmployeeDepartmentDTO> response = employeeClient.getEmployeesByDepartmentId(departmentId);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return ResponseEntity.status(response.getStatusCode()).build();
        }

        EmployeeDepartmentDTO fullDto = response.getBody();

        // Filter to include only employeeId and displayName
        List<EmployeeTeamResponse> filteredEmployees = fullDto.getEmployeeList()
            .stream()
            .map(emp -> {
                EmployeeTeamResponse minimal = new EmployeeTeamResponse();
                minimal.setEmployeeId(emp.getEmployeeId());
                minimal.setDisplayName(emp.getDisplayName());
                return minimal;
            })
            .collect(Collectors.toList());

        // Set filtered list
        fullDto.setEmployeeList(filteredEmployees);

        return ResponseEntity.ok(fullDto);
    }

    public boolean existsByDepartmentId(String departmentId) {
        try {
            ResponseEntity<EmployeeDepartmentDTO> response = getEmployeesByDeptId(departmentId);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEmployeeInDepartment(String employeeId, String deptId) {
        ResponseEntity<EmployeeDepartmentDTO> response = getEmployeesByDeptId(deptId);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return false;
        }

        List<EmployeeTeamResponse> employees = response.getBody().getEmployeeList();

        return employees.stream()
            .map(EmployeeTeamResponse::getEmployeeId)
            .collect(Collectors.toSet())
            .contains(employeeId);
    }

    public ResponseEntity<EmployeeDepartmentDTO> getDepartmentByEmployeeId(String employeeId) {
        // TODO implement if needed
        return null;
    }

    public ResponseEntity<EmployeeDepartmentDTO> getDepartmentById(String groupId) {
        // TODO implement if needed
        return null;
    }
}
