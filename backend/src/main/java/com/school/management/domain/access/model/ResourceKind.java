package com.school.management.domain.access.model;

/**
 * 资源种类 (统一锚定模型第一根轴) —— 决定一个资源的归属如何存储, 优先于基数。
 *
 * <ul>
 *   <li>{@link #SUBJECT} —— 记录本身就是三大主体之一 (user / org_unit / place, 含
 *       user_student 这类挂在 user 上的扩展)。它对另一主体的关系是<b>主体↔主体的边</b>,
 *       本就存在 {@code access_relations} 图里, 引擎走主体图, <b>不</b>注册列锚 —— 无论单值多值。</li>
 *   <li>{@link #PLAIN} —— 记录不是主体 (检查记录 / 考勤 / 成绩…)。其归属按基数落列 (单值) 或
 *       {@code record_relations} (多值)。</li>
 * </ul>
 *
 * <p>持久化到 {@code data_resources.resource_kind}, 以 {@code name()} 往返。
 */
public enum ResourceKind {
    SUBJECT, PLAIN;

    /** 按 {@code name()} 严格解析, 未知/null 回退 {@link #PLAIN} (普通记录是安全默认)。 */
    public static ResourceKind fromCode(String c) {
        if (c != null) {
            for (ResourceKind k : values()) {
                if (k.name().equals(c)) return k;
            }
        }
        return PLAIN;
    }
}
