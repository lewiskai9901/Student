package com.school.management.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 当前数据库中的密码哈希
        String storedHash = "$2a$10$N.zmdr9k7uOCQb7f1ibJIe.IDwcRuRJy9lqe8rlQx5pAUUyoV3hc6";
        String password = "123456";
        
        System.out.println("Testing password: " + password);
        System.out.println("Stored hash: " + storedHash);
        
        boolean matches = encoder.matches(password, storedHash);
        System.out.println("Password matches: " + matches);
        
        // 生成新的哈希
        String newHash = encoder.encode(password);
        System.out.println("\nNew hash for 123456: " + newHash);
        
        // 验证新生成的哈希
        boolean newMatches = encoder.matches(password, newHash);
        System.out.println("New hash matches: " + newMatches);
    }
}
