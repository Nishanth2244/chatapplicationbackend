package com.app.chat_service.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.chat_service.service.ImageService;

@RestController
@RequestMapping("/api/chat")
public class ImageController {
	
	@Autowired
	private ImageService imageService;
	
	@GetMapping("/{employeeId}/image")
	//@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR', 'MANAGER','TEAM_LEAD')")
	public CompletableFuture<ResponseEntity<String>> getEmployeeImage(@PathVariable("employeeId") String employeeId) throws Exception{
	        return imageService.getEmployeeImage(employeeId)
	                .thenApply(ResponseEntity::ok);
	}

}
