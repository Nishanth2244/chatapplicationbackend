package com.app.chat_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.app.chat_service.model.employee_details;
import com.app.chat_service.repo.EmployeeDetailsRepository;

@Service
public class EmployeeDetailsService {

    @Autowired
    private EmployeeDetailsRepository employeeRepository;

    // Add Employee
    @CachePut(value = "employees", key = "#employee.employeeId")
    public employee_details addEmployee(employee_details employee) {
        return employeeRepository.save(employee);
    }

    // Update Employee
    @CachePut(value = "employees", key = "#employeeId")
    public employee_details updateEmployee(String employeeId, employee_details updatedEmployee) {
        Optional<employee_details> existing = employeeRepository.findById(employeeId);
        if (existing.isPresent()) {
            employee_details emp = existing.get();
            emp.setEmployeeName(updatedEmployee.getEmployeeName());
            emp.setProfileLink(updatedEmployee.getProfileLink());
            return employeeRepository.save(emp);
        }
        return null; 
    }



    // Get Employee by ID
    @Cacheable(value = "employees", key = "#employeeId")
    public employee_details getEmployeeById(String employeeId) {
        return employeeRepository.findById(employeeId).orElse(null);
    }

    
//    Delete Emp by Id
	public void deleteById(String employeeId) {
		employeeRepository.deleteById(employeeId);
		
	}
    
}
