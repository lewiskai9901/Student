package com.school.management.domain.scoring.model.aggregate;

import com.school.management.domain.scoring.model.valueobject.RuleType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 计算规则聚合根
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculationRule {

    private Long id;

    /**
     * 规则代码（唯一标识）
     */
    private String code;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 规则类型
     */
    private RuleType ruleType;

    /**
     * 条件公式（JavaScript），返回boolean
     */
    private String conditionFormula;

    /**
     * 动作公式（JavaScript），返回调整后的分数
     */
    private String actionFormula;

    /**
     * 规则参数定义
     */
    private Map<String, Object> parametersSchema;

    /**
     * 默认参数
     */
    private Map<String, Object> defaultParameters;

    /**
     * 执行优先级（数字越小越先执行）
     */
    private Integer priority;

    /**
     * 匹配后是否停止后续规则
     */
    private Boolean stopOnMatch;

    /**
     * 是否系统内置
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 审计字段
     */
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    /**
     * 创建自定义规则
     */
    public static CalculationRule createCustom(String code, String name, String description,
                                                RuleType ruleType, String conditionFormula,
                                                String actionFormula, Integer priority, Boolean stopOnMatch) {
        return CalculationRule.builder()
                .code(code)
                .name(name)
                .description(description)
                .ruleType(ruleType)
                .conditionFormula(conditionFormula)
                .actionFormula(actionFormula)
                .priority(priority)
                .stopOnMatch(stopOnMatch != null ? stopOnMatch : false)
                .isSystem(false)
                .isEnabled(true)
                .build();
    }

    /**
     * 启用规则
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用规则
     */
    public void disable() {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统内置规则不能禁用");
        }
        this.isEnabled = false;
    }

    /**
     * 更新公式
     */
    public void updateFormulas(String conditionFormula, String actionFormula) {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统内置规则不能修改公式");
        }
        this.conditionFormula = conditionFormula;
        this.actionFormula = actionFormula;
    }

    /**
     * 更新优先级
     */
    public void updatePriority(Integer priority) {
        this.priority = priority;
    }
}
