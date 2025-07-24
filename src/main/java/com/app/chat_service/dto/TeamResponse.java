package com.app.chat_service.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamResponse {

    private String teamId;
    private String teamName;
    private List<EmployeeTeamResponse> employees;

}
