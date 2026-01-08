package com.school.management.util;

import com.school.management.common.result.ResultCode;
import com.school.management.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码强度验证器
 *
 * @author system
 * @since 1.0.0
 */
public final class PasswordValidator {

    private PasswordValidator() {
        // 私有构造函数，防止实例化
    }

    /** 最小密码长度 */
    private static final int MIN_LENGTH = 8;

    /** 最大密码长度 */
    private static final int MAX_LENGTH = 32;

    /** 包含数字的正则 */
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    /** 包含小写字母的正则 */
    private static final Pattern HAS_LOWERCASE = Pattern.compile(".*[a-z].*");

    /** 包含大写字母的正则 */
    private static final Pattern HAS_UPPERCASE = Pattern.compile(".*[A-Z].*");

    /** 包含特殊字符的正则 */
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    /** 常见弱密码列表 */
    private static final List<String> WEAK_PASSWORDS = List.of(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",
            "baseball", "iloveyou", "master", "sunshine", "ashley",
            "bailey", "passw0rd", "shadow", "123123", "654321",
            "superman", "qazwsx", "michael", "football", "password1",
            "password123", "admin123", "admin", "root", "test123"
    );

    /**
     * 验证密码强度
     *
     * @param password 密码
     * @throws BusinessException 如果密码不符合要求
     */
    public static void validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, "密码不能为空");
        }

        // 检查长度
        if (password.length() < MIN_LENGTH) {
            errors.add("密码长度至少" + MIN_LENGTH + "位");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("密码长度不能超过" + MAX_LENGTH + "位");
        }

        // 检查复杂度（至少满足3项）
        int complexity = 0;
        if (HAS_DIGIT.matcher(password).matches()) complexity++;
        if (HAS_LOWERCASE.matcher(password).matches()) complexity++;
        if (HAS_UPPERCASE.matcher(password).matches()) complexity++;
        if (HAS_SPECIAL.matcher(password).matches()) complexity++;

        if (complexity < 3) {
            errors.add("密码必须包含大写字母、小写字母、数字、特殊字符中的至少3种");
        }

        // 检查是否为弱密码
        if (WEAK_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("密码过于简单，请使用更复杂的密码");
        }

        // 检查是否包含连续字符
        if (hasConsecutiveChars(password, 4)) {
            errors.add("密码不能包含4个及以上连续字符（如1234、abcd）");
        }

        // 检查是否包含重复字符
        if (hasRepeatingChars(password, 4)) {
            errors.add("密码不能包含4个及以上重复字符（如aaaa、1111）");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR,
                "密码不符合安全要求: " + String.join("; ", errors));
        }
    }

    /**
     * 检查是否有连续字符
     */
    private static boolean hasConsecutiveChars(String password, int minLength) {
        if (password.length() < minLength) return false;

        for (int i = 0; i <= password.length() - minLength; i++) {
            boolean isConsecutive = true;
            for (int j = 1; j < minLength; j++) {
                if (password.charAt(i + j) != password.charAt(i + j - 1) + 1) {
                    isConsecutive = false;
                    break;
                }
            }
            if (isConsecutive) return true;
        }
        return false;
    }

    /**
     * 检查是否有重复字符
     */
    private static boolean hasRepeatingChars(String password, int minLength) {
        if (password.length() < minLength) return false;

        for (int i = 0; i <= password.length() - minLength; i++) {
            boolean isRepeating = true;
            char c = password.charAt(i);
            for (int j = 1; j < minLength; j++) {
                if (password.charAt(i + j) != c) {
                    isRepeating = false;
                    break;
                }
            }
            if (isRepeating) return true;
        }
        return false;
    }

    /**
     * 获取密码强度级别
     *
     * @param password 密码
     * @return 强度级别: 0-弱, 1-中, 2-强, 3-非常强
     */
    public static int getStrengthLevel(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return 0;
        }

        int score = 0;

        // 长度加分
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;

        // 复杂度加分
        if (HAS_DIGIT.matcher(password).matches()) score++;
        if (HAS_LOWERCASE.matcher(password).matches()) score++;
        if (HAS_UPPERCASE.matcher(password).matches()) score++;
        if (HAS_SPECIAL.matcher(password).matches()) score++;

        // 弱密码减分
        if (WEAK_PASSWORDS.contains(password.toLowerCase())) {
            return 0;
        }

        if (score <= 2) return 0;
        if (score <= 4) return 1;
        if (score <= 6) return 2;
        return 3;
    }
}
