package one.june.leave_management.common.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Utility class for executing code asynchronously.
 */
@Component
@Slf4j
public class AsyncUtility {

    /**
     * Execute a Runnable asynchronously.
     *
     * @param runnable The runnable to execute
     */
    @Async("taskExecutor")
    public void executeAsync(Runnable runnable) {
        log.debug("Executing async task");
        runnable.run();
        log.debug("Async task completed successfully");
    }
}
