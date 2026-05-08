package com.school.management.application.access.masking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通用字段脱敏服务. 提供常见字段类型的 mask 实现.
 *
 * <p>使用规则:
 * <ul>
 *   <li>{@link #maskPhone(String)} 11 位手机号 → 前 3 + ****+ 后 4: "13800000001" → "138****0001"</li>
 *   <li>{@link #maskEmail(String)} 邮箱 → local 首字母 + *** + @domain: "alice@x.com" → "a***@x.com"</li>
 *   <li>{@link #maskIdCard(String)} 身份证 → 前 3 + **** + 后 4(已在 UserDomainResponse 实现, 这里提供工具方法供其他场景复用)</li>
 *   <li>{@link #maskName(String)} 中文姓名 → 首字 + *: "王浩宇" → "王**宇", 单字保留</li>
 * </ul>
 *
 * <p>所有方法对 null/空字符串/不合规输入幂等返回原值,不抛异常.
 */
@Slf4j
@Service
public class MaskingService {

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        // 手机号至少 7 位才有意义脱敏: 前 3 + 中间星 + 后 4
        int prefix = 3, suffix = 4;
        if (phone.length() < prefix + suffix) return phone;
        return phone.substring(0, prefix) + "****" + phone.substring(phone.length() - suffix);
    }

    public String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at < 1) return email;  // 没有 @ 或 @ 在首位 → 不脱敏
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() == 1) return local + "***" + domain;
        return local.charAt(0) + "***" + domain;
    }

    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 3) + "****" + idCard.substring(idCard.length() - 4);
    }

    public String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        // 3 字以上: 首 + ** + 末
        return name.charAt(0) + "**" + name.charAt(name.length() - 1);
    }

    /** 通用:按字段名分发到对应 mask 方法. */
    public String maskField(String fieldName, String value) {
        if (fieldName == null || value == null) return value;
        return switch (fieldName) {
            case "phone" -> maskPhone(value);
            case "email" -> maskEmail(value);
            case "idCard" -> maskIdCard(value);
            case "name", "realName" -> maskName(value);
            default -> value;  // 未知字段不动
        };
    }
}
