package softarch.restaurant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Async configuration for Spring @Async event listeners.
 *
 * Used by:
 *   - KitchenItemDoneListener (inventory auto-deduct after ticket DONE)
 *     → Must not block the kitchen ticket update transaction
 *
 * Thread pool sizing:
 *   - corePoolSize=4   : base threads always alive (handles normal load)
 *   - maxPoolSize=16   : burst capacity for peak hours
 *   - queueCapacity=100: buffer before spawning extra threads
 *
 * Shutdown:
 *   - waitForTasksToCompleteOnShutdown=true : graceful drain on app stop
 *   - awaitTerminationSeconds=30            : max wait before forcing shutdown
 *
 * Implements AsyncConfigurer so getAsyncUncaughtExceptionHandler()
 * logs all @Async method failures instead of swallowing them silently.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("restaurant-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // Caller-runs policy: if queue is full, the calling thread executes the task
        // (ensures no events are silently dropped under heavy load)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Catches exceptions thrown inside @Async methods.
     * Without this, exceptions from async listeners are swallowed silently.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
            log.error(
                "[ASYNC ERROR] Method={}.{}() | Params={} | Error={}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                Arrays.toString(params),
                ex.getMessage(),
                ex
            );
    }
}
