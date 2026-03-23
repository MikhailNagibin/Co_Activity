package com.coactivity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution in the notification service. Enables background
 * email sending without blocking the main application threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

  /**
   * Creates a thread pool executor for async notification tasks.
   *
   * <p>Configuration:
   * <ul>
   *   <li>Core pool size: 2 threads (minimum active threads)</li>
   *   <li>Max pool size: 5 threads (maximum concurrent email sends)</li>
   *   <li>Queue capacity: 100 (pending email tasks)</li>
   *   <li>Thread name prefix: "notification-" for easy log identification</li>
   * </ul>
   *
   * @return configured executor for async operations
   */
  @Bean(name = "taskExecutor")
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("notification-");
    executor.initialize();
    return executor;
  }

  /**
   * Provides exception handler for uncaught exceptions in async methods. Logs the exception details
   * for debugging and monitoring.
   *
   * @return the exception handler
   */
  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncUncaughtExceptionHandler() {
      @Override
      public void handleUncaughtException(@NonNull Throwable ex, @NonNull Method method,
          @NonNull Object... params) {
        log.error("Uncaught exception in async method '{}': {}", method.getName(),
            ex.getMessage(), ex);
      }
    };
  }
}
