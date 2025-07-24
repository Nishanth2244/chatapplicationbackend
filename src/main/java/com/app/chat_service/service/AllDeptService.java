package com.app.chat_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.chat_service.dto.DepartmentDTO;
import com.app.chat_service.feignclient.EmployeeClient;

@Service
public class AllDeptService {

    @Autowired
    private EmployeeClient employeeClient;

    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        List<DepartmentDTO> departments = employeeClient.getAllDepartments();
        return new ResponseEntity<>(departments, HttpStatus.OK);
    }
}
