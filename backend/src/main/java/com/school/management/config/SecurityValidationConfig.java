package com.school.management.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Slf4j
@Configuration
public class SecurityValidationConfig {

    private static final Set<String> FORBIDDEN_PASSWORDS = Set.of(
        "123456", "password", "admin", "root", "test", "demo",
        "123456789", "12345678", "1234567890", "qwerty"
    );

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @PostConstruct
    public void validateOnStartup() {
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
