package com.school.management.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityValidationConfig {

    private static final Set<String> FORBIDDEN_PASSWORDS = Set.of(
        "123456", "password", "admin", "root", "test", "demo",
        "123456789", "12345678", "1234567890", "qwerty"
    );

    private final Environment environment;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @PostConstruct
    public void validateOnStartup() {
        // 开发环境跳过密码强度验证
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (isDev) {
            log.warn("Security validation skipped in dev profile");
            return;
        }
        validateDatabasePassword(dbPassword);
        log.info("Security validation passed: database password is not a default value");
    }

    public void validateDatabasePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                "Database password is not configured. " +
                "Please set DB_PASSWORD environment variable."
            );
        }

        if (FORBIDDEN_PASSWORDS.contains(password.toLowerCase())) {
            throw new IllegalStateException(
                "SECURITY RISK: Database password is set to a common default value. " +
                "Please change DB_PASSWORD to a secure password before starting the application."
            );
        }

        if (password.length() < 8) {
            throw new IllegalStateException(
                "Database password is too short. " +
                "Please use a password with at least 8 characters."
            );
        }
    }
}
