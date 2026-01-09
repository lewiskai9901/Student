package com.school.management.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class SecurityValidationConfigTest {

    @Test
    @DisplayName("当数据库密码为默认值时应抛出异常")
    void shouldRejectDefaultPassword() {
        SecurityValidationConfig config = new SecurityValidationConfig();
        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("123456");
        });
    }

    @Test
    @DisplayName("当数据库密码为安全值时应通过验证")
    void shouldAcceptSecurePassword() {
        SecurityValidationConfig config = new SecurityValidationConfig();
        assertDoesNotThrow(() -> {
            config.validateDatabasePassword("MySecureP@ssw0rd!");
        });
    }

    @Test
    @DisplayName("当数据库密码为空时应抛出异常")
    void shouldRejectEmptyPassword() {
        SecurityValidationConfig config = new SecurityValidationConfig();
        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("");
        });
    }

    @Test
    @DisplayName("当数据库密码过短时应抛出异常")
    void shouldRejectShortPassword() {
        SecurityValidationConfig config = new SecurityValidationConfig();
        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("abc123");
        });
    }
}
