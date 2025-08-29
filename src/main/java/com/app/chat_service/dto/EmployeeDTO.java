package com.app.chat_service.dto;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
 
 
 
import java.time.LocalDate;
import java.util.*;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmployeeDTO {
 
 
    @NotBlank(message = "Employee Id is required")
    @Pattern(regexp = "^ACS\\d{8}$", message = "Employee ID must start with 'ACS' followed by 8 digits ")
    private String employeeId;
 
    @NotBlank(message = "Employee frist name is required")
    private String firstName;
    private String middleName;
    private String lastName;
    private String displayName;
    private String maritalStatus;
    private String bloodGroup;
    private String physicallyHandicapped;
    private String nationality;
    private String gender;
    private LocalDate dateOfBirth;
 
    private String employeeImage;
 
 
    private String workEmail;
    private String personalEmail;
    private String mobileNumber;
    private String workNumber;
 
    private String street;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String district;
 
    private LocalDate dateOfJoining;
    private LocalDate dateOfLeaving;
    private String jobTitlePrimary;
    private String jobTitleSecondary;
    private String inProbation;
    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private String probationPolicy;
    private String workingType;
    private String timeType;
    private String contractStatus;
    private LocalDate contractStartDate;
 
    private String location;
    private List<String> Skills;
 
    private String departmentId;
 
 
}