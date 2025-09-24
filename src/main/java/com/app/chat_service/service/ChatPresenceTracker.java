package com.app.chat_service.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ChatPresenceTracker {

    // Key: userId, Value: Set of open chat targets
    private final Map<String, Set<String>> activeWindows = new ConcurrentHashMap<>();

    public void openChat(String userId, String target) {
        // Create or get the existing Set<String>, thread-safe
        activeWindows
            .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
            .add(target);
    }

    public void closeChat(String userId, String target) {
        activeWindows
            .getOrDefault(userId, Collections.emptySet())
            .remove(target);
    }

    public boolean isChatWindowOpen(String userId, String target) {
        return activeWindows
            .getOrDefault(userId, Collections.emptySet())
            .contains(target);
    }

    public Set<String> getOpenWindows(String userId) {
        return activeWindows
            .getOrDefault(userId, Collections.emptySet());
    }
}
