package com.school.management.domain.access.model.valueobject;

/**
 * 关系访问等级 (value object).
 *
 * 表达 subject 对 resource 的操作授权深度,与 relation 类型独立 —
 * relation 描述"是什么关系",accessLevel 描述"该关系下能做多少".
 *
 * DB column access_relations.access_level 是 VARCHAR(20),按 enum.name() 存储.
 */
public enum AccessLevel {
    /** 只读 — 仅查询,不改 */
    READ_ONLY,
    /** 读写 — 大部分业务关系默认 */
    FULL,
    /** 拥有 — 包含 FULL + 能再授权(grant/revoke 子权限) */
    OWNER;

    /**
     * 容错解析:大小写不敏感,空值/非法值返回 FULL.
     * 历史数据(旧 TINYINT 时期 1/2/0 已被迁移脚本统一改成 'FULL') 不会进此路径,
     * 但留 fallback 防异常入侵.
     */
    public static AccessLevel parse(String s) {
        if (s == null || s.isBlank()) return FULL;
        try {
            return AccessLevel.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FULL;
        }
    }

    /** 当前等级是否包含写权限. */
    public boolean isReadWrite() {
        return this == FULL || this == OWNER;
    }

    /** 当前等级是否能再授权给他人. */
    public boolean canDelegate() {
        return this == OWNER;
    }
}
