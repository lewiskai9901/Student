package com.school.management.domain.place.model.valueobject;

import lombok.Getter;

/**
 * 性别类型枚举
 */
@Getter
public enum GenderType {
    MIXED(0, "不限"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String description;

    GenderType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static GenderType fromCode(Integer code) {
        if (code == null) {
            return MIXED; // 默认不限
        }
        for (GenderType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return MIXED; // 未知值默认为不限
    }

    /**
     * 从场所 gender 列字符串 (MALE/FEMALE/MIXED, NULL) 解析。未知/空 → MIXED (不限)。
     * 场所 gender 持久化为枚举名字符串, 与本枚举 name() 对齐。
     */
    public static GenderType fromName(String name) {
        if (name == null || name.isBlank()) {
            return MIXED;
        }
        try {
            return GenderType.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MIXED;
        }
    }

    /**
     * 此性别限制是否允许给定性别码的用户入住。
     *
     * <p>注意 code 对齐 users.gender 口径 (1=男, 2=女): MALE.code==1, FEMALE.code==2,
     * 故可直接与 user.getGender() 比对。MIXED 恒允许; 受限场所仅精确匹配才允许,
     * 未知性别 (null) 一律拒绝受限场所。
     *
     * @param occupantGenderCode 入住者性别码 (1=男, 2=女; null=未知)
     */
    public boolean allowsOccupant(Integer occupantGenderCode) {
        if (this == MIXED) return true;
        return occupantGenderCode != null && this.code == occupantGenderCode;
    }

    /**
     * 检查性别是否与此类型匹配
     * @param gender 性别描述 ("男"/"女")
     */
    public boolean matchesGender(String gender) {
        if (this == MIXED) return true;
        if (gender == null) return false;
        return this.description.equals(gender);
    }

    /**
     * 是否限制性别
     */
    public boolean isRestricted() {
        return this != MIXED;
    }
}
