package com.app.chat_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDepartmentDTO {

    private String departmentId;
    private String departmentName;
//    private String employeeId;
//    private String employeeName;
//    private String employeeEmail;

    private List<EmployeeTeamResponse> employeeList;
}
