package com.app.chat_service.config;
 
import com.app.chat_service.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
 
import java.util.Map;
 
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
 
    private final OnlineUserService onlineUserService;
    // SimpMessagingTemplate is needed to send messages to WebSocket topics
    private final SimpMessagingTemplate messagingTemplate;
 
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        String userId = event.getUser() != null ? event.getUser().getName() : null;
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
 
        if (userId != null) {
            onlineUserService.addUser(userId, sessionId);
            log.info("User connected: {} (sessionId={})", userId, sessionId);
 
            // --- FIX START: Broadcast that the user is now ONLINE ---
            // Create a payload to send to the frontend
            Map<String, Object> statusPayload = Map.of(
                    "userId", userId,
                    "isOnline", true
            );
            // Send the payload to a general topic that all clients listen to
            messagingTemplate.convertAndSend("/topic/presence", statusPayload);
            // --- FIX END ---
 
        } else {
            log.warn("Connection without employeeId in Principal");
        }
    }
 
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String employeeId = event.getUser() != null ? event.getUser().getName() : null;
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
 
        if (employeeId != null) {
            onlineUserService.removeUser(employeeId, sessionId);
            log.info("User disconnected: {} (sessionId={})", employeeId, sessionId);
 
            // --- FIX START: Broadcast that the user is now OFFLINE ---
            // Create a payload to send to the frontend
            Map<String, Object> statusPayload = Map.of(
                    "userId", employeeId,
                    "isOnline", false
            );
            // Send the payload to the same general topic
            messagingTemplate.convertAndSend("/topic/presence", statusPayload);
            // --- FIX END ---
 
        } else {
            log.warn("Disconnection without employeeId in Principal");
        }
    }
}