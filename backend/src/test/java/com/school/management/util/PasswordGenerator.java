package com.school.management.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成密码 123456 的哈希
        String password = "123456";
        String hash = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println();

        // 验证哈希
        boolean matches = encoder.matches(password, hash);
        System.out.println("Password matches: " + matches);
        System.out.println();

        // 测试数据库中现有的哈希
        String existingHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lFZ6NMyoKQHb55sTW";
        System.out.println("Testing existing hash from database:");
        System.out.println("Hash: " + existingHash);

        boolean existingMatches = encoder.matches(password, existingHash);
        System.out.println("Matches '123456': " + existingMatches);

        // 测试其他常见密码
        String[] testPasswords = {"admin", "admin123", "Admin123", "123456"};
        System.out.println("\nTesting multiple passwords against existing hash:");
        for (String pwd : testPasswords) {
            boolean match = encoder.matches(pwd, existingHash);
            System.out.println("Password '" + pwd + "': " + (match ? "MATCH" : "no match"));
        }

        // 生成SQL更新语句
        System.out.println("\n=== SQL Update Statement ===");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'admin';");
    }
}
