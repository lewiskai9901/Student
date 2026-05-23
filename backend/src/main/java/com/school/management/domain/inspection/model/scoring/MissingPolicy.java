package com.school.management.domain.inspection.model.scoring;

/**
 * 当目标在某源分区下无提交时的处理策略.
 *
 * <p>注: DB 历史默认值是 'SKIP'(旧 COMPOSITE 指标用), 代码层把 SKIP 视为 IGNORE 同义,
 * 见 {@link #fromString(String)}.
 */
public enum MissingPolicy {
    /** 忽略该分区, 不计入聚合 (等价于 'SKIP') */
    IGNORE,
    /** 视为 0 分 */
    ZERO,
    /** 视为该分区最高分 */
    MAX,
    /** 等待该分区有提交后再评级 */
    WAIT;

    /** SKIP / IGNORE / null 统一归一到 IGNORE; 其他原样解析. */
    public static MissingPolicy fromString(String raw) {
        if (raw == null) return IGNORE;
        String s = raw.trim().toUpperCase();
        if (s.isEmpty() || "SKIP".equals(s)) return IGNORE;
        try {
            return MissingPolicy.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return IGNORE;
        }
    }
}
