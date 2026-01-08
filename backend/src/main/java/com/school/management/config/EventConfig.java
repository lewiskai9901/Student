package com.school.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Event infrastructure configuration.
 *
 * <p>Enables:
 * <ul>
 *   <li>Scheduled processing for the Outbox pattern</li>
 *   <li>Async event processing (if enabled)</li>
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

    // Configuration is primarily done via application.yml
    // This class enables scheduling for OutboxProcessor

}
