package com.school.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for domain event handling.
 *
 * Enables asynchronous event processing for domain events,
 * improving performance by decoupling event publishing from handling.
 *
 * Note: DomainEventPublisher is created via @Component annotation
 * on SpringDomainEventPublisher class.
 */
@Configuration
@EnableAsync
public class DomainEventConfig {
    // Bean definitions handled by component scanning
}
