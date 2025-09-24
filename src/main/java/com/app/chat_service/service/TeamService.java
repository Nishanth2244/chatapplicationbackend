package com.app.chat_service.service;

import java.lang.System.Logger;
import org.springframework.cache.annotation.Caching; 
import org.springframework.cache.annotation.CacheEvict;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.chat_service.dto.GroupChatDetailsResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.repo.ChatMessageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TeamService {

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /** Check if a given team exists */
    @Cacheable(value = "teamExists", key = "#teamId")
    public boolean existsByTeamId(String teamId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Get all teams where an employee belongs */
    @Cacheable(value = "employeeTeams", key = "#employeeId")
    
    public List<TeamResponse> getTeamsByEmployeeId(String employeeId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamAllEmployees(employeeId);
            log.info("got teams {}",response);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


    /** Get team details by teamId */
    @Cacheable(value = "teamDetails", key = "#teamId")
    public TeamResponse getGroupById(String teamId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                return response.getBody().get(0);
            }
        } catch (Exception e) {
            // log error
        }
        return null;
    }

    /** Get all members of a group */
    @Cacheable(value = "groupMembers", key = "#teamId")
    public List<TeamResponse> getGroupMembers(String teamId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            log.info("fetched emplyeess by Team id {}",response);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
        	log.info("exception occured while fetching the members");
            return Collections.emptyList();
        }
    }

    /** Get just the employee IDs of a given team */
    @Cacheable(value = "teamMembers", key = "#teamId")
    public List<String> getEmployeeIdsByTeamId(String teamId) {
        List<TeamResponse> teams = getGroupMembers(teamId);
        if (teams.isEmpty()) {
            return Collections.emptyList();
        }
        return teams.get(0).getEmployees()
                .stream()
                .map(emp -> emp.getEmployeeId())
                .collect(Collectors.toList());
    }
    
    
    public ResponseEntity<List<TeamResponse>> ByEmpId(String employeeId) {
        try {
            // Call Feign client directly
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamAllEmployees(employeeId);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody() != null ? response.getBody() : Collections.emptyList());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyList());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
    
//    Method for deleting the data in Redis Cache
    
    @Caching(evict = {
            @CacheEvict(value = "teamExists", key = "#teamId"),
            @CacheEvict(value = "teamDetails", key = "#teamId"),
            @CacheEvict(value = "groupMembers", key = "#teamId"),
            @CacheEvict(value = "teamMembers", key = "#teamId")
        })
        public void evictTeamCaches(String teamId) {
            log.info("Evicting all caches for teamId: {}", teamId);
        }

    
//  Method for cleaning the teams of an employee belongs
    
    @CacheEvict(value = "employeeTeams", key = "#employeeId")
	public void evictEmployeeTeamsCache(String employeeId) {
		log.info("evicting the old teams of an employee from cache of : {}", employeeId);
	}

}