package com.app.chat_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentDTO {

    @NotBlank(message="DepartmentId is required")
    @Pattern(regexp = "^DEP\\d{3}$",message = "departmentId  should start with DEP followed by 3 digits")
    private String departmentId;
    private String departmentName;
    private String departmentDescription;
}
