package com.school.management.domain.organization.model;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 专业方向实体
 * 属于 Major 聚合，表示同一专业的不同培养层次
 * 例如：计算机应用专业 -> 3+2高级工班、5年制技师班
 */
public class MajorDirection extends Entity<Long> {

    private Long id;

    /**
     * 方向编码
     */
    private String directionCode;

    /**
     * 方向名称
     */
    private String directionName;

    /**
     * 层次（中级工/高级工/预备技师/技师）
     */
    private String level;

    /**
     * 学制（年）
     */
    private Integer years;

    /**
     * 是否分段注册
     */
    private Boolean segmented;

    /**
     * 第一阶段层次
     */
    private String phase1Level;

    /**
     * 第一阶段学制（年）
     */
    private Integer phase1Years;

    /**
     * 第二阶段层次
     */
    private String phase2Level;

    /**
     * 第二阶段学制（年）
     */
    private Integer phase2Years;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 是否启用
     */
    private Boolean enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For JPA/MyBatis
    protected MajorDirection() {
    }

    private MajorDirection(String directionCode, String directionName, String level, Integer years,
                           boolean segmented, String phase1Level, Integer phase1Years,
                           String phase2Level, Integer phase2Years) {
        this.directionCode = Objects.requireNonNull(directionCode, "directionCode cannot be null");
        this.directionName = Objects.requireNonNull(directionName, "directionName cannot be null");
        this.level = level;
        this.years = years;
        this.segmented = segmented;
        this.phase1Level = phase1Level;
        this.phase1Years = phase1Years;
        this.phase2Level = phase2Level;
        this.phase2Years = phase2Years;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        validate();
    }

    /**
     * 工厂方法
     */
    public static MajorDirection create(String directionCode, String directionName,
                                         String level, Integer years,
                                         boolean segmented, String phase1Level, Integer phase1Years,
                                         String phase2Level, Integer phase2Years) {
        return new MajorDirection(directionCode, directionName, level, years,
            segmented, phase1Level, phase1Years, phase2Level, phase2Years);
    }

    /**
     * 用于从数据库重建
     */
    public static MajorDirection reconstitute(Long id, String directionCode, String directionName,
                                               String level, Integer years,
                                               boolean segmented, String phase1Level, Integer phase1Years,
                                               String phase2Level, Integer phase2Years,
                                               String remarks, Boolean enabled,
                                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        MajorDirection direction = new MajorDirection();
        direction.id = id;
        direction.directionCode = directionCode;
        direction.directionName = directionName;
        direction.level = level;
        direction.years = years;
        direction.segmented = segmented;
        direction.phase1Level = phase1Level;
        direction.phase1Years = phase1Years;
        direction.phase2Level = phase2Level;
        direction.phase2Years = phase2Years;
        direction.remarks = remarks;
        direction.enabled = enabled != null ? enabled : true;
        direction.createdAt = createdAt;
        direction.updatedAt = updatedAt;
        return direction;
    }

    /**
     * 更新专业方向信息
     */
    public void update(String directionName, String level, Integer years,
                       boolean segmented, String phase1Level, Integer phase1Years,
                       String phase2Level, Integer phase2Years) {
        if (directionName != null && !directionName.isBlank()) {
            this.directionName = directionName;
        }
        this.level = level;
        this.years = years;
        this.segmented = segmented;
        this.phase1Level = phase1Level;
        this.phase1Years = phase1Years;
        this.phase2Level = phase2Level;
        this.phase2Years = phase2Years;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置备注
     */
    public void setRemarks(String remarks) {
        this.remarks = remarks;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用
     */
    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 禁用
     */
    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取总学制（年）
     */
    public int getTotalYears() {
        if (segmented != null && segmented) {
            int p1 = phase1Years != null ? phase1Years : 0;
            int p2 = phase2Years != null ? phase2Years : 0;
            return p1 + p2;
        }
        return years != null ? years : 0;
    }

    /**
     * 获取层次显示文本
     */
    public String getLevelDisplay() {
        if (segmented != null && segmented) {
            return phase1Level + " → " + phase2Level;
        }
        return level;
    }

    /**
     * 获取学制显示文本
     */
    public String getYearsDisplay() {
        if (segmented != null && segmented) {
            return phase1Years + "+" + phase2Years + "年";
        }
        return years + "年";
    }

    private void validate() {
        if (directionCode == null || directionCode.isBlank()) {
            throw new IllegalArgumentException("Direction code cannot be empty");
        }
        if (directionCode.length() > 50) {
            throw new IllegalArgumentException("Direction code cannot exceed 50 characters");
        }
        if (directionName == null || directionName.isBlank()) {
            throw new IllegalArgumentException("Direction name cannot be empty");
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDirectionCode() {
        return directionCode;
    }

    public String getDirectionName() {
        return directionName;
    }

    public String getLevel() {
        return level;
    }

    public Integer getYears() {
        return years;
    }

    public Boolean isSegmented() {
        return segmented;
    }

    public String getPhase1Level() {
        return phase1Level;
    }

    public Integer getPhase1Years() {
        return phase1Years;
    }

    public String getPhase2Level() {
        return phase2Level;
    }

    public Integer getPhase2Years() {
        return phase2Years;
    }

    public String getRemarks() {
        return remarks;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
