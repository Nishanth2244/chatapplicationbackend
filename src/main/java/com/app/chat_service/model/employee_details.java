package com.app.chat_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_details")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class employee_details {

    @Id
    private String employeeId;
    private String employeeName;
    
    @Column(columnDefinition = "TEXT")
    private String profileLink;
}
