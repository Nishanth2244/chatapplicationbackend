package com.app.chat_service.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.chat_service.model.employee_details;
import com.app.chat_service.repo.EmployeeDetailsRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ImageService {
	
	@Autowired
	private EmployeeDetailsRepository employeeDetailsRepository;
	
	@Autowired
	private S3Service s3Service;
	
	public CompletableFuture<String> getEmployeeImage(String employeeId) throws Exception {
	    log.info("Fetching image for employeeId={} on thread={}", employeeId, Thread.currentThread().getName());
	    employee_details employee = employeeDetailsRepository.findById(employeeId)
	            .orElseThrow(() -> new Exception("Employee Not Found with id: " + employeeId));

	    String s3Key = employee.getProfileLink();
	    if (s3Key == null || s3Key.isBlank()) {
	        log.warn("No image found for employeeId={}", employeeId);
	        throw new Exception("No image uploaded for employee: " + employeeId);
	    }

	    return CompletableFuture.completedFuture(s3Service.generatePresignedUrl(s3Key));
	}
	 

}
