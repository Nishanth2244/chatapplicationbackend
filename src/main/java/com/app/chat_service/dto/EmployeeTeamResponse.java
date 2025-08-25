package com.app.chat_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeTeamResponse {

    private String employeeId;
    private String displayName;
    private String jobTitlePrimary;
    private String workEmail;
    private String workNumber;
    private String employeeImage;

}
