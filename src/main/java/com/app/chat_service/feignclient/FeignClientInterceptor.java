package com.app.chat_service.feignclient;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // ✅ FIX: Check if a request context actually exists
        if (RequestContextHolder.getRequestAttributes() != null) {
        	
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && !authHeader.isEmpty()) {
                log.info("Got header and forwarding it: {}", authHeader);
                template.header("Authorization", authHeader);
            }
        } else {
            // This will be logged when called from WebSocket or other background threads
            log.warn("Cannot forward token: No request attributes found in this thread.");
        }
    }
}