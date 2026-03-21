package com.school.management.domain.inspection.model.v7.scoring;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 评选等级实体 — 每个评选规则可配置多个从高到低判定的等级
 * conditionLogic: AND / OR
 * conditions: JSON 数组，每项为 EvaluationCondition 结构
 */
public class EvaluationLevel implements Entity<Long> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;
    private Long ruleId;
    private Integer levelNum;       // 1=最高
    private String levelName;
    private String levelIcon;
    private String levelColor;
    private String conditionLogic;  // AND / OR
    private String conditions;      // JSON
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected EvaluationLevel() {
    }

    private EvaluationLevel(Builder builder) {
        this.id = builder.id;
        this.ruleId = builder.ruleId;
        this.levelNum = builder.levelNum;
        this.levelName = builder.levelName;
        this.levelIcon = builder.levelIcon;
        this.levelColor = builder.levelColor;
        this.conditionLogic = builder.conditionLogic != null ? builder.conditionLogic : "AND";
        this.conditions = builder.conditions;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static EvaluationLevel reconstruct(Builder builder) {
        return new EvaluationLevel(builder);
    }

    /**
     * 解析 conditions JSON 为条件列表
     */
    public List<EvaluationCondition> parseConditions() {
        if (conditions == null || conditions.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(conditions,
                    new TypeReference<List<EvaluationCondition>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }
    public Long getRuleId() { return ruleId; }
    public Integer getLevelNum() { return levelNum; }
    public String getLevelName() { return levelName; }
    public String getLevelIcon() { return levelIcon; }
    public String getLevelColor() { return levelColor; }
    public String getConditionLogic() { return conditionLogic; }
    public String getConditions() { return conditions; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long ruleId;
        private Integer levelNum;
        private String levelName;
        private String levelIcon;
        private String levelColor;
        private String conditionLogic;
        private String conditions;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder ruleId(Long ruleId) { this.ruleId = ruleId; return this; }
        public Builder levelNum(Integer levelNum) { this.levelNum = levelNum; return this; }
        public Builder levelName(String levelName) { this.levelName = levelName; return this; }
        public Builder levelIcon(String levelIcon) { this.levelIcon = levelIcon; return this; }
        public Builder levelColor(String levelColor) { this.levelColor = levelColor; return this; }
        public Builder conditionLogic(String conditionLogic) { this.conditionLogic = conditionLogic; return this; }
        public Builder conditions(String conditions) { this.conditions = conditions; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public EvaluationLevel build() { return new EvaluationLevel(this); }
    }
}
