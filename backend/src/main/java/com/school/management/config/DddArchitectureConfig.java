package com.school.management.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for DDD Architecture components.
 *
 * This configuration enables scanning of:
 * - Domain layer components (domain services)
 * - Application layer components (application services)
 * - Infrastructure layer components (repositories, adapters)
 * - Interface layer components (REST controllers)
 * - MyBatis mappers for the new v2 architecture
 */
@Configuration
@ComponentScan(basePackages = {
    "com.school.management.domain",
    "com.school.management.application",
    "com.school.management.infrastructure",
    "com.school.management.interfaces"
})
@MapperScan(basePackages = {
    "com.school.management.infrastructure.persistence.organization",
    "com.school.management.infrastructure.persistence.inspection",
    "com.school.management.infrastructure.persistence.access",
    "com.school.management.infrastructure.persistence.asset",
    "com.school.management.infrastructure.persistence.task",
    "com.school.management.infrastructure.persistence.student",
    "com.school.management.infrastructure.persistence.rating",
    "com.school.management.infrastructure.persistence.semester",
    "com.school.management.infrastructure.persistence.user",
    "com.school.management.infrastructure.persistence.query",
    "com.school.management.infrastructure.audit"
})
public class DddArchitectureConfig {
    // Configuration is handled through annotations
}
