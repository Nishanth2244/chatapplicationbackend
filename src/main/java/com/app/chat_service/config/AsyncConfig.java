package com.app.chat_service.config; // Or your common config package

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // This ensures the feature is turned on
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Start with 5 threads
        executor.setMaxPoolSize(20); // Allow up to 10 threads
        executor.setQueueCapacity(35); // Queue up to 25 tasks before rejecting
        executor.setThreadNamePrefix("AsyncProcessor-"); // Crucial for logging!
        executor.initialize();
        return executor;
    }
}