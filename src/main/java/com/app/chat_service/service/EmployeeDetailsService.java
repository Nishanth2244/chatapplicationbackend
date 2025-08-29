package com.app.chat_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.chat_service.model.employee_details;
import com.app.chat_service.repo.EmployeeDetailsRepository;

@Service
public class EmployeeDetailsService {

    @Autowired
    private EmployeeDetailsRepository employeeRepository;

    // ✅ Add Employee
    public employee_details addEmployee(employee_details employee) {
        return employeeRepository.save(employee);
    }

    // ✅ Update Employee
    public employee_details updateEmployee(String employeeId, employee_details updatedEmployee) {
        Optional<employee_details> existing = employeeRepository.findById(employeeId);
        if (existing.isPresent()) {
            employee_details emp = existing.get();
            emp.setEmployeeName(updatedEmployee.getEmployeeName());
            emp.setProfileLink(updatedEmployee.getProfileLink());
            return employeeRepository.save(emp);
        }
        return null; // or throw exception
    }

    // ✅ Delete Employee
    public String deleteEmployee(String employeeId) {
        if (employeeRepository.existsById(employeeId)) {
            employeeRepository.deleteById(employeeId);
            return "Employee deleted successfully";
        }
        return "Employee not found";
    }

    // ✅ Get Employee by ID
    public employee_details getEmployeeById(String employeeId) {
        return employeeRepository.findById(employeeId).orElse(null);
    }

    // ✅ Get All Employees
    public List<employee_details> getAllEmployees() {
        return employeeRepository.findAll();
    }
    
    public Boolean existById(String EmployeeId) {
    	return employeeRepository.findById(EmployeeId)!=null ? true : false;
    }
}
