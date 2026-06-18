package com.school.management.domain.access.model;

/**
 * 关系基数 —— 一个资源的某条具名关系指向多少个主体。
 *
 * <ul>
 *   <li>{@link #SINGLE} —— 单值 (创建者 / 所属组织 / 单一被检查对象)。</li>
 *   <li>{@link #MULTI} —— 多值 (复核员 / 分享给多人 / 一次检查多个被检查组织)。</li>
 * </ul>
 *
 * <p>仅在 {@link ResourceKind#PLAIN} 资源上影响存储推导 (见 {@link StorageKind#derive})。
 */
public enum Cardinality {
    SINGLE, MULTI;

    /** 按 {@code name()} 严格解析, 未知/null 回退 {@link #SINGLE}。 */
    public static Cardinality fromCode(String c) {
        if (c != null) {
            for (Cardinality v : values()) {
                if (v.name().equals(c)) return v;
            }
        }
        return SINGLE;
    }
}
