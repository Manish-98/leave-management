package one.june.leave_management.common.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

/**
 * Custom exception handler for asynchronous methods.
 * Handles uncaught exceptions thrown during async method execution.
 */
@Slf4j
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        log.error("Async method {} threw exception: {}",
                method.getName(),
                throwable.getMessage(),
                throwable);

        // Log parameters if available
        if (params != null && params.length > 0) {
            log.error("Method parameters:");
            for (int i = 0; i < params.length; i++) {
                log.error("  Param {}: {}", i, params[i]);
            }
        }

        // You can add additional error handling here, such as:
        // - Sending notifications
        // - Writing to a dead letter queue
        // - Updating database with failure records
    }
}
