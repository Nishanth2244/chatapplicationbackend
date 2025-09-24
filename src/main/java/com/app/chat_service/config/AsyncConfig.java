package com.app.chat_service.config; // Or your common config package

import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // This ensures the feature is turned on
public class AsyncConfig {

    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Start with 5 threads
        executor.setMaxPoolSize(20); // Allow up to 10 threads
        executor.setQueueCapacity(35); // Queue up to 25 tasks before rejecting
        executor.setThreadNamePrefix("AsyncProcessor-"); // Crucial for logging!
        
     // This decorator will copy the request context to the async threads
        executor.setTaskDecorator(new ContextCopyingDecorator());
        
        executor.initialize();
        return executor;
    }
    
 // Helper class to copy the context
    class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            RequestAttributes context = RequestContextHolder.currentRequestAttributes();
            return () -> {
                try {
                    RequestContextHolder.setRequestAttributes(context);
                    runnable.run();
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            };
        }
    }
}