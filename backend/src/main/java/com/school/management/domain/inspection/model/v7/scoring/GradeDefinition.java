package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;

/**
 * V7 等级定义 — 属于 GradeScheme 聚合
 */
public class GradeDefinition implements Entity<Long> {

    private Long id;
    private Long gradeSchemeId;
    private String code;
    private String name;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String color;
    private String icon;
    private Integer sortOrder;

    protected GradeDefinition() {
    }

    private GradeDefinition(Builder builder) {
        this.id = builder.id;
        this.gradeSchemeId = builder.gradeSchemeId;
        this.code = builder.code;
        this.name = builder.name;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.color = builder.color;
        this.icon = builder.icon;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
    }

    // ── Factory ──────────────────────────────────────────────

    public static GradeDefinition create(Long gradeSchemeId, String code, String name,
                                         BigDecimal minValue, BigDecimal maxValue,
                                         String color, String icon, Integer sortOrder) {
        GradeDefinition def = new GradeDefinition();
        def.gradeSchemeId = gradeSchemeId;
        def.code = code;
        def.name = name;
        def.minValue = minValue;
        def.maxValue = maxValue;
        def.color = color;
        def.icon = icon;
        def.sortOrder = sortOrder != null ? sortOrder : 0;
        return def;
    }

    public static GradeDefinition reconstruct(Builder builder) {
        return new GradeDefinition(builder);
    }

    // ── Getters ──────────────────────────────────────────────

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGradeSchemeId() {
        return gradeSchemeId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    // ── Builder ──────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long gradeSchemeId;
        private String code;
        private String name;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String color;
        private String icon;
        private Integer sortOrder;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder gradeSchemeId(Long gradeSchemeId) { this.gradeSchemeId = gradeSchemeId; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder minValue(BigDecimal minValue) { this.minValue = minValue; return this; }
        public Builder maxValue(BigDecimal maxValue) { this.maxValue = maxValue; return this; }
        public Builder color(String color) { this.color = color; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }

        public GradeDefinition build() {
            return new GradeDefinition(this);
        }
    }
}
