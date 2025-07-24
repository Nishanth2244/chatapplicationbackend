package com.app.chat_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Store online users
    private static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        // You can extract userId from headers/authentication
        String userId = extractUserId(event);
        onlineUsers.add(userId);
        System.out.println("User connected: " + userId);
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String userId = extractUserId(event);
        onlineUsers.remove(userId);
        System.out.println("User disconnected: " + userId);
    }

    public static boolean isUserOnline(String userId) {
        return onlineUsers.contains(userId);
    }

    private String extractUserId(SessionConnectEvent event) {
        // TODO: Extract from headers or Principal
        return "EMP001"; // For now static
    }

    private String extractUserId(SessionDisconnectEvent event) {
        // TODO: Extract userId if available
        return "EMP001";
    }
}
