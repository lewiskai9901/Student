package com.school.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Event infrastructure configuration.
 *
 * <p>Enables:
 * <ul>
 *   <li>Scheduled processing for the Outbox pattern</li>
 *   <li>Async event processing (if enabled)</li>
 *   <li>TaskScheduler bean for delayed retries (e.g. CorrectiveAutoCreationHandler)</li>
 * </ul>
 *
 * <p>Configuration properties:
 * <pre>
 * app:
 *   events:
 *     sync-publish: true           # Enable synchronous event publishing
 *   outbox:
 *     batch-size: 100              # Events processed per batch
 *     max-retries: 5               # Max retry attempts
 *     process-interval: 5000       # Processing interval in ms
 *     cleanup-days: 7              # Days to keep published events
 *     cleanup-cron: "0 0 0 * * ?"  # Cleanup schedule
 * </pre>
 */
@Configuration
@EnableScheduling
public class EventConfig {

    /**
     * 显式声明 TaskScheduler bean — 用于业务代码的延迟调度 (如自动整改重试).
     * 默认 Spring 会用单线程 SimpleAsyncTaskScheduler, 不适合并发延迟任务.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("biz-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
