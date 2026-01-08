package com.school.management.domain.organization.model;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 专业聚合根
 * 管理专业及其下属的专业方向
 */
public class Major extends AggregateRoot<Long> {

    private Long id;

    /**
     * 专业编码
     */
    private String majorCode;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 所属组织单元ID（系部）
     */
    private Long orgUnitId;

    /**
     * 专业描述
     */
    private String description;

    /**
     * 专业方向列表
     */
    private List<MajorDirection> directions;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序号
     */
    private Integer sortOrder;

    // 审计字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    // For JPA/MyBatis
    protected Major() {
        this.directions = new ArrayList<>();
    }

    private Major(Builder builder) {
        this.id = builder.id;
        this.majorCode = Objects.requireNonNull(builder.majorCode, "majorCode cannot be null");
        this.majorName = Objects.requireNonNull(builder.majorName, "majorName cannot be null");
        this.orgUnitId = Objects.requireNonNull(builder.orgUnitId, "orgUnitId cannot be null");
        this.description = builder.description;
        this.directions = builder.directions != null ? new ArrayList<>(builder.directions) : new ArrayList<>();
        this.enabled = builder.enabled != null ? builder.enabled : true;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = builder.createdBy;

        validate();
    }

    /**
     * 工厂方法：创建新专业
     */
    public static Major create(String majorCode, String majorName, Long orgUnitId,
                               String description, Long createdBy) {
        return builder()
            .majorCode(majorCode)
            .majorName(majorName)
            .orgUnitId(orgUnitId)
            .description(description)
            .createdBy(createdBy)
            .build();
    }

    /**
     * 更新专业信息
     */
    public void updateInfo(String majorName, String description, Integer sortOrder, Long updatedBy) {
        if (majorName != null && !majorName.isBlank()) {
            this.majorName = majorName;
        }
        this.description = description;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加专业方向
     */
    public MajorDirection addDirection(String directionCode, String directionName,
                                        String level, Integer years,
                                        boolean isSegmented, String phase1Level, Integer phase1Years,
                                        String phase2Level, Integer phase2Years) {
        // 检查编码是否已存在
        if (directions.stream().anyMatch(d -> d.getDirectionCode().equals(directionCode))) {
            throw new IllegalArgumentException("Direction code already exists: " + directionCode);
        }

        MajorDirection direction = MajorDirection.create(
            directionCode, directionName, level, years,
            isSegmented, phase1Level, phase1Years, phase2Level, phase2Years
        );
        this.directions.add(direction);
        this.updatedAt = LocalDateTime.now();
        return direction;
    }

    /**
     * 更新专业方向
     */
    public void updateDirection(Long directionId, String directionName,
                                String level, Integer years,
                                boolean isSegmented, String phase1Level, Integer phase1Years,
                                String phase2Level, Integer phase2Years) {
        MajorDirection direction = findDirectionById(directionId)
            .orElseThrow(() -> new IllegalArgumentException("Direction not found: " + directionId));

        direction.update(directionName, level, years, isSegmented,
            phase1Level, phase1Years, phase2Level, phase2Years);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 移除专业方向
     */
    public void removeDirection(Long directionId) {
        boolean removed = directions.removeIf(d -> d.getId().equals(directionId));
        if (!removed) {
            throw new IllegalArgumentException("Direction not found: " + directionId);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 根据ID查找专业方向
     */
    public Optional<MajorDirection> findDirectionById(Long directionId) {
        return directions.stream()
            .filter(d -> d.getId().equals(directionId))
            .findFirst();
    }

    /**
     * 根据编码查找专业方向
     */
    public Optional<MajorDirection> findDirectionByCode(String directionCode) {
        return directions.stream()
            .filter(d -> d.getDirectionCode().equals(directionCode))
            .findFirst();
    }

    /**
     * 启用专业
     */
    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 禁用专业
     */
    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 转移到其他组织单元
     */
    public void transferToOrgUnit(Long newOrgUnitId, Long updatedBy) {
        this.orgUnitId = Objects.requireNonNull(newOrgUnitId, "newOrgUnitId cannot be null");
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    private void validate() {
        if (majorCode == null || majorCode.isBlank()) {
            throw new IllegalArgumentException("Major code cannot be empty");
        }
        if (majorCode.length() > 50) {
            throw new IllegalArgumentException("Major code cannot exceed 50 characters");
        }
        if (majorName == null || majorName.isBlank()) {
            throw new IllegalArgumentException("Major name cannot be empty");
        }
        if (majorName.length() > 100) {
            throw new IllegalArgumentException("Major name cannot exceed 100 characters");
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

    public String getMajorCode() {
        return majorCode;
    }

    public String getMajorName() {
        return majorName;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getDescription() {
        return description;
    }

    public List<MajorDirection> getDirections() {
        return Collections.unmodifiableList(directions);
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String majorCode;
        private String majorName;
        private Long orgUnitId;
        private String description;
        private List<MajorDirection> directions;
        private Boolean enabled;
        private Integer sortOrder;
        private Long createdBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder majorCode(String majorCode) {
            this.majorCode = majorCode;
            return this;
        }

        public Builder majorName(String majorName) {
            this.majorName = majorName;
            return this;
        }

        public Builder orgUnitId(Long orgUnitId) {
            this.orgUnitId = orgUnitId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder directions(List<MajorDirection> directions) {
            this.directions = directions;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Major build() {
            return new Major(this);
        }
    }
}
