package com.school.management.domain.scoring.model.aggregate;

import com.school.management.domain.scoring.model.valueobject.StrategyCategory;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 计分策略聚合根
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringStrategy {

    private Long id;

    /**
     * 策略代码（唯一标识）
     */
    private String code;

    /**
     * 策略名称
     */
    private String name;

    /**
     * 策略描述
     */
    private String description;

    /**
     * 策略分类
     */
    private StrategyCategory category;

    /**
     * JavaScript公式模板
     */
    private String formulaTemplate;

    /**
     * 公式说明（如：得分 = 基准分 - Σ扣分）
     */
    private String formulaDescription;

    /**
     * 策略参数定义（JSON Schema格式）
     */
    private Map<String, Object> parametersSchema;

    /**
     * 默认参数值
     */
    private Map<String, Object> defaultParameters;

    /**
     * 支持的打分方式代码列表
     */
    private List<String> supportedInputTypes;

    /**
     * 支持的计算规则类型
     */
    private List<String> supportedRuleTypes;

    /**
     * 是否系统内置
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 审计字段
     */
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    /**
     * 创建自定义策略
     */
    public static ScoringStrategy createCustom(String code, String name, String description,
                                                StrategyCategory category, String formulaTemplate,
                                                String formulaDescription, List<String> supportedInputTypes) {
        return ScoringStrategy.builder()
                .code(code)
                .name(name)
                .description(description)
                .category(category)
                .formulaTemplate(formulaTemplate)
                .formulaDescription(formulaDescription)
                .supportedInputTypes(supportedInputTypes)
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(100)
                .build();
    }

    /**
     * 启用策略
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用策略
     */
    public void disable() {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统内置策略不能禁用");
        }
        this.isEnabled = false;
    }

    /**
     * 更新公式
     */
    public void updateFormula(String formulaTemplate, String formulaDescription) {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统内置策略不能修改公式");
        }
        this.formulaTemplate = formulaTemplate;
        this.formulaDescription = formulaDescription;
    }

    /**
     * 检查是否支持指定的打分方式
     */
    public boolean supportsInputType(String inputTypeCode) {
        return supportedInputTypes != null && supportedInputTypes.contains(inputTypeCode);
    }
}
