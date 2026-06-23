package com.school.management.domain.access.model.chain;

/**
 * 多级关系链 (统一锚定 P0) 同级多关系的组合方式。
 *
 * <ul>
 *   <li>{@code AND} —— 交集 (同时满足多个关系, 如"负责 且 管理"的组织)。</li>
 *   <li>{@code OR}  —— 并集 (满足任一关系, 如"成员 或 管理"的组织)。</li>
 * </ul>
 *
 * <p>设计: docs/plans/2026-06-23-multi-level-chain-FINAL-plan.md §1.3。
 */
public enum Combine {
    AND,
    OR;

    /** 宽松解析: 空/非法 → OR (并集, 较安全的默认)。 */
    public static Combine fromCode(String code) {
        if (code == null) return OR;
        return "AND".equalsIgnoreCase(code.trim()) ? AND : OR;
    }
}
