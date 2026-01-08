package com.school.management.controller;

import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 开发辅助控制器 - 仅在开发环境(dev profile)中激活
 * 生产环境自动禁用，无需手动删除
 *
 * @security 此控制器仅在 spring.profiles.active=dev 时可用
 */
@Slf4j
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile("dev")  // 仅在dev环境激活，生产环境自动禁用
@Tag(name = "开发辅助", description = "开发环境专用接口(仅dev环境可用)")
public class DevController {

    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 重置admin密码为admin123
     */
    @PostMapping("/reset-admin-password")
    @Operation(summary = "重置admin密码", description = "将admin用户密码重置为admin123")
    public Result<String> resetAdminPassword() {
        try {
            String password = "admin123";
            String encodedPassword = passwordEncoder.encode(password);

            log.debug("密码哈希生成成功"); // 不记录实际哈希值

            // 更新数据库
            int updated = jdbcTemplate.update(
                "UPDATE users SET password = ? WHERE username = 'admin'",
                encodedPassword
            );

            if (updated > 0) {
                // 验证密码
                String storedPassword = jdbcTemplate.queryForObject(
                    "SELECT password FROM users WHERE username = 'admin'",
                    String.class
                );

                boolean matches = passwordEncoder.matches(password, storedPassword);

                return Result.success(String.format(
                    "密码重置成功！\n" +
                    "用户名: admin\n" +
                    "密码: %s\n" +
                    "BCrypt哈希: %s\n" +
                    "验证结果: %s",
                    password,
                    encodedPassword,
                    matches ? "✓ 成功" : "✗ 失败"
                ));
            } else {
                return Result.error("未找到admin用户");
            }
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error("重置密码失败: " + e.getMessage());
        }
    }

    /**
     * 验证密码
     */
    @GetMapping("/verify-password")
    @Operation(summary = "验证密码", description = "验证指定密码是否与数据库中的哈希匹配")
    public Result<String> verifyPassword(
        @RequestParam String username,
        @RequestParam String password
    ) {
        try {
            String storedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = ? AND deleted = 0",
                String.class,
                username
            );

            boolean matches = passwordEncoder.matches(password, storedPassword);

            return Result.success(String.format(
                "验证结果:\n" +
                "用户名: %s\n" +
                "输入密码: %s\n" +
                "数据库哈希: %s\n" +
                "匹配结果: %s",
                username,
                password,
                storedPassword,
                matches ? "✓ 密码正确" : "✗ 密码错误"
            ));
        } catch (Exception e) {
            log.error("验证密码失败", e);
            return Result.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 生成BCrypt哈希
     */
    @GetMapping("/generate-hash")
    @Operation(summary = "生成BCrypt哈希", description = "为指定密码生成BCrypt哈希值")
    public Result<String> generateHash(@RequestParam String password) {
        try {
            String hash = passwordEncoder.encode(password);
            boolean verify = passwordEncoder.matches(password, hash);

            return Result.success(String.format(
                "密码: %s\n" +
                "BCrypt哈希: %s\n" +
                "自验证: %s\n\n" +
                "SQL语句:\n" +
                "UPDATE users SET password = '%s' WHERE username = 'admin';",
                password,
                hash,
                verify ? "✓ 成功" : "✗ 失败",
                hash
            ));
        } catch (Exception e) {
            log.error("生成哈希失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }
}
