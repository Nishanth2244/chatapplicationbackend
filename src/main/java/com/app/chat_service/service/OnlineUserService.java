package com.app.chat_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private final SimpMessagingTemplate messagingTemplate;

    /** userId -> active session IDs */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /** Add session for a user */
    public void addUser(String userId, String sessionId) {
        userSessions.compute(userId, (id, sessions) -> {
            if (sessions == null) {
                sessions = ConcurrentHashMap.newKeySet();
            }
            sessions.add(sessionId);
            return sessions;
        });
        // Only broadcast online when this is their first active session
        if (userSessions.get(userId).size() == 1) {
            broadcastPresence(userId, true);
        }
    }

    /** Remove session for a user */
    public void removeUser(String userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                broadcastPresence(userId, false);
            }
        }
    }

    /** Check if user has at least one active session */
    public boolean isOnline(String userId) {
        return userSessions.containsKey(userId);
    }

    /** Get all currently online user IDs */
    public Set<String> getAllOnlineUsers() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    /** Send WebSocket presence update */
    private void broadcastPresence(String userId, boolean isOnline) {
        messagingTemplate.convertAndSend(
            "/topic/presence",
            new PresenceUpdate(userId, isOnline)
        );
    }

    /** Inner DTO for WebSocket presence updates */
    public record PresenceUpdate(String userId, boolean online) { }
}
