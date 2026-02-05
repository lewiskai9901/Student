package com.school.management.domain.scoring.model.aggregate;

import com.school.management.domain.scoring.model.valueobject.ComponentType;
import com.school.management.domain.scoring.model.valueobject.ValueType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 打分方式聚合根
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputType {

    private Long id;

    /**
     * 打分方式代码（唯一标识）
     */
    private String code;

    /**
     * 打分方式名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 分类：basic/extended
     */
    private String category;

    /**
     * UI组件类型
     */
    private ComponentType componentType;

    /**
     * 组件配置参数
     */
    private Map<String, Object> componentConfig;

    /**
     * 值类型
     */
    private ValueType valueType;

    /**
     * 值映射规则
     */
    private Map<String, Object> valueMapping;

    /**
     * 值验证规则
     */
    private Map<String, Object> validationRules;

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
     * 创建自定义打分方式
     */
    public static InputType createCustom(String code, String name, String description,
                                          String category, ComponentType componentType,
                                          Map<String, Object> componentConfig,
                                          ValueType valueType, Map<String, Object> valueMapping,
                                          Map<String, Object> validationRules) {
        return InputType.builder()
                .code(code)
                .name(name)
                .description(description)
                .category(category != null ? category : "extended")
                .componentType(componentType)
                .componentConfig(componentConfig)
                .valueType(valueType)
                .valueMapping(valueMapping)
                .validationRules(validationRules)
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(100)
                .build();
    }

    /**
     * 启用
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用
     */
    public void disable() {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统内置打分方式不能禁用");
        }
        this.isEnabled = false;
    }

    /**
     * 更新配置
     */
    public void updateConfig(Map<String, Object> componentConfig, Map<String, Object> validationRules) {
        if (Boolean.TRUE.equals(this.isSystem)) {
            throw new IllegalStateException("系统内置打分方式不能修改配置");
        }
        this.componentConfig = componentConfig;
        this.validationRules = validationRules;
    }
}
