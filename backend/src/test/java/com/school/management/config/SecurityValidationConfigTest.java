package com.school.management.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityValidationConfigTest {

    private Environment environment;
    private SecurityValidationConfig config;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        config = new SecurityValidationConfig(environment);
    }

    @Test
    @DisplayName("当数据库密码为默认值时应抛出异常")
    void shouldRejectDefaultPassword() {
        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("123456");
        });
    }

    @Test
    @DisplayName("当数据库密码为安全值时应通过验证")
    void shouldAcceptSecurePassword() {
        assertDoesNotThrow(() -> {
            config.validateDatabasePassword("MySecureP@ssw0rd!");
        });
    }

    @Test
    @DisplayName("当数据库密码为空时应抛出异常")
    void shouldRejectEmptyPassword() {
        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("");
        });
    }

    @Test
    @DisplayName("当数据库密码过短时应抛出异常")
    void shouldRejectShortPassword() {
        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("abc123");
        });
    }
}
