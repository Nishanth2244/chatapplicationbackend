package com.app.chat_service.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.chat_service.model.employee_details;

@Repository
public interface EmployeeDetailsRepository extends JpaRepository<employee_details, String> {
}
