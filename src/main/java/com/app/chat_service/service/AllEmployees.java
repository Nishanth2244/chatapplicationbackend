package com.app.chat_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.chat_service.dto.EmployeeDTO;
import com.app.chat_service.feignclient.EmployeeClient;

@Service
public class AllEmployees {

    @Autowired
    private EmployeeClient employeeClient;

    public ResponseEntity<EmployeeDTO> getEmployeeById(String id) {
        ResponseEntity<EmployeeDTO> response = employeeClient.getEmployeeById(id);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return ResponseEntity.notFound().build();
        }

        EmployeeDTO fullEmployee = response.getBody();

        EmployeeDTO limitedEmployee = new EmployeeDTO();
        limitedEmployee.setEmployeeId(fullEmployee.getEmployeeId());
        limitedEmployee.setDisplayName(fullEmployee.getDisplayName());

        return ResponseEntity.ok(limitedEmployee);
    }

    public boolean existsById(String employeeId) {
        try {
            ResponseEntity<EmployeeDTO> response = getEmployeeById(employeeId);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
        } catch (Exception e) {
            System.out.println("Failed to check employee by ID: " + e.getMessage());
            return false;
        }
    }
}
