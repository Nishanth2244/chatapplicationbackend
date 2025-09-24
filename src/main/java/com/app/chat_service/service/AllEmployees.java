package com.app.chat_service.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.model.employee_details;
import com.app.chat_service.repo.EmployeeDetailsRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AllEmployees {

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private EmployeeDetailsRepository employeeDetailsRepository;
    
    @Autowired
    private S3Service s3Service;

    @Cacheable(value = "employeeDetails", key = "#id", unless="#result == null")
    public employee_details getEmployeeById(String id) {
        Optional<employee_details> optionalEmployee = employeeDetailsRepository.findById(id);

        if (optionalEmployee.isPresent()) {
            employee_details employee = optionalEmployee.get();
            log.info("Original S3 Key from DB: {}", employee.getProfileLink());

            // Check if the profileLink is a valid S3 key and not already a URL
            if (employee.getProfileLink() != null && !employee.getProfileLink().isBlank() && !employee.getProfileLink().startsWith("http")) {
                
                // 1. Generate the presigned URL from the S3 key
                String presignedUrl = s3Service.generatePresignedUrl(employee.getProfileLink());

                // 2. Create a NEW, detached employee object to avoid DB updates
                employee_details responseDto = new employee_details();
                responseDto.setEmployeeId(employee.getEmployeeId());
                responseDto.setEmployeeName(employee.getEmployeeName());
                
                // 3. Set the temporary presigned URL on the new object
                responseDto.setProfileLink(presignedUrl); 
                
                log.info("Returning employee with presigned URL: {}", responseDto);
                return responseDto; // Return the new object with the URL

            } else if (employee.getProfileLink() != null && employee.getProfileLink().startsWith("http")) {
                // This is a sign the DB is already corrupted with a URL.
                log.warn("Profile link for employee {} is already a URL. It should be an S3 key. Returning it as is for now.", id);
                return employee;
            }
            
            // Return the original employee if there's no profile link
            return employee;
            
        } else {
            return null;
        }
    }

    public boolean existsById(String employeeId) {
        try {
            employee_details emp = getEmployeeById(employeeId);
            return emp != null;
        } catch (Exception e) {
            log.error("Failed to check employee by ID {}: {}", employeeId, e.getMessage());
            return false;
        }
    }
}