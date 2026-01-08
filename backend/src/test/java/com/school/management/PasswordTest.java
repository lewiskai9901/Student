package com.school.management;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 测试密码
        String[] passwords = {"123456", "admin", "admin123"};
        String existingHash = "$2a$10$7JB720yubVSOfvVHdJGNcOE4vAh7XnQsQKvJCtvJfMZQKrEL5aCPi";

        System.out.println("测试现有hash:");
        for (String pwd : passwords) {
            boolean matches = encoder.matches(pwd, existingHash);
            System.out.println(pwd + " -> " + matches);
        }

        System.out.println("\n生成新的hash:");
        System.out.println("123456 -> " + encoder.encode("123456"));
        System.out.println("admin -> " + encoder.encode("admin"));
    }
}
