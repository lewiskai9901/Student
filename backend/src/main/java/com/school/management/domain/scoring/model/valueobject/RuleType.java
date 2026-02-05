package com.school.management.domain.scoring.model.valueobject;

import lombok.Getter;

/**
 * 计算规则类型
 */
@Getter
public enum RuleType {

    /**
     * 封顶规则
     */
    CEILING("ceiling", "分数封顶"),

    /**
     * 保底规则
     */
    FLOOR("floor", "分数保底"),

    /**
     * 一票否决
     */
    VETO("veto", "一票否决"),

    /**
     * 累进规则
     */
    PROGRESSIVE("progressive", "累进惩罚"),

    /**
     * 奖励规则
     */
    BONUS("bonus", "奖励加分"),

    /**
     * 惩罚规则
     */
    PENALTY("penalty", "惩罚扣分");

    private final String code;
    private final String name;

    RuleType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static RuleType fromCode(String code) {
        for (RuleType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return CEILING;
    }
}
