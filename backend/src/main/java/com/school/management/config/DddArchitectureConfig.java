package com.school.management.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for DDD Architecture components.
 *
 * 只负责 ComponentScan. @MapperScan 归一到 StudentManagementApplication 上,
 * 避免和主 app 扫描重叠导致 "Bean already defined with the same name" 警告
 * (Phase 6.3 启动优化, 2026-04-20).
 */
@Configuration
@ComponentScan(basePackages = {
    "com.school.management.domain",
    "com.school.management.application",
    "com.school.management.infrastructure",
    "com.school.management.interfaces"
})
public class DddArchitectureConfig {
    // Configuration is handled through annotations
}
