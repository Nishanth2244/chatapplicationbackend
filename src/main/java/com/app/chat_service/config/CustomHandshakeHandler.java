package com.app.chat_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;

@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        // Extract employeeId from query parameters
        String employeeId = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("employeeId");

        // Fallback if not provided
        if (employeeId == null || employeeId.isBlank()) {
            employeeId = "anonymous-" + System.currentTimeMillis();
            log.warn("No employeeId provided in WebSocket handshake, assigned anonymous ID: {}", employeeId);
        } else {
            log.info("WebSocket handshake established for employeeId: {}", employeeId);
        }

        final String principalName = employeeId;
        return () -> principalName;  // Return Principal with employeeId
    }
}
