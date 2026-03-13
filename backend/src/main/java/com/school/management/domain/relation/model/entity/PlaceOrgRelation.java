package com.school.management.domain.relation.model.entity;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Space-Organization Relation Entity
 * Supports multi-organization attribution for spaces
 */
public class SpaceOrgRelation implements Entity<Long> {

    private Long id;
    private Long spaceId;
    private Long orgUnitId;
    private RelationType relationType;
    private boolean isPrimary;
    private Integer priorityLevel;
    private boolean canUse;
    private boolean canManage;
    private boolean canAssign;
    private boolean canInspect;
    private String useSchedule;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer allocatedCapacity;
    private BigDecimal weightRatio;
    private Integer sortOrder;
    private String remark;
    private Long createdBy;

    public enum RelationType {
        PRIMARY("Primary"),
        SHARED("Shared"),
        MANAGED("Managed");

        private final String label;

        RelationType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // Default constructor
    public SpaceOrgRelation() {}

    // Builder pattern constructor
    private SpaceOrgRelation(Builder builder) {
        this.id = builder.id;
        this.spaceId = builder.spaceId;
        this.orgUnitId = builder.orgUnitId;
        this.relationType = builder.relationType;
        this.isPrimary = builder.isPrimary;
        this.priorityLevel = builder.priorityLevel;
        this.canUse = builder.canUse;
        this.canManage = builder.canManage;
        this.canAssign = builder.canAssign;
        this.canInspect = builder.canInspect;
        this.useSchedule = builder.useSchedule;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.allocatedCapacity = builder.allocatedCapacity;
        this.weightRatio = builder.weightRatio;
        this.sortOrder = builder.sortOrder;
        this.remark = builder.remark;
        this.createdBy = builder.createdBy;
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public Long getSpaceId() { return spaceId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public RelationType getRelationType() { return relationType; }
    public boolean isPrimary() { return isPrimary; }
    public Integer getPriorityLevel() { return priorityLevel; }
    public boolean isCanUse() { return canUse; }
    public boolean isCanManage() { return canManage; }
    public boolean isCanAssign() { return canAssign; }
    public boolean isCanInspect() { return canInspect; }
    public String getUseSchedule() { return useSchedule; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getAllocatedCapacity() { return allocatedCapacity; }
    public BigDecimal getWeightRatio() { return weightRatio; }
    public Integer getSortOrder() { return sortOrder; }
    public String getRemark() { return remark; }
    public Long getCreatedBy() { return createdBy; }

    // Domain methods
    public void updatePermissions(boolean canUse, boolean canManage, boolean canAssign, boolean canInspect) {
        this.canUse = canUse;
        this.canManage = canManage;
        this.canAssign = canAssign;
        this.canInspect = canInspect;
    }

    public void updatePriority(Integer priorityLevel) {
        this.priorityLevel = priorityLevel != null ? priorityLevel : 1;
    }

    public void updateSchedule(String useSchedule) {
        this.useSchedule = useSchedule;
    }

    public void updatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateCapacity(Integer allocatedCapacity) {
        if (allocatedCapacity != null && allocatedCapacity < 0) {
            throw new IllegalArgumentException("Allocated capacity cannot be negative");
        }
        this.allocatedCapacity = allocatedCapacity;
    }

    public void updateWeight(BigDecimal weightRatio) {
        if (weightRatio != null && (weightRatio.compareTo(BigDecimal.ZERO) < 0 || weightRatio.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("Weight ratio should be between 0 and 100");
        }
        this.weightRatio = weightRatio;
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public void setAsPrimary() {
        this.isPrimary = true;
    }

    public void clearPrimary() {
        this.isPrimary = false;
    }

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        boolean afterStart = startDate == null || !today.isBefore(startDate);
        boolean beforeEnd = endDate == null || !today.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    public boolean isExpired() {
        return endDate != null && LocalDate.now().isAfter(endDate);
    }

    public boolean isExpiringSoon(int days) {
        if (endDate == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !isExpired() && !endDate.isAfter(warningDate);
    }

    public boolean hasFullManagementRights() {
        return canUse && canManage && canAssign && canInspect;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long spaceId;
        private Long orgUnitId;
        private RelationType relationType;
        private boolean isPrimary;
        private Integer priorityLevel;
        private boolean canUse;
        private boolean canManage;
        private boolean canAssign;
        private boolean canInspect;
        private String useSchedule;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer allocatedCapacity;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;
        private Long createdBy;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder spaceId(Long spaceId) { this.spaceId = spaceId; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder relationType(RelationType relationType) { this.relationType = relationType; return this; }
        public Builder isPrimary(boolean isPrimary) { this.isPrimary = isPrimary; return this; }
        public Builder priorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; return this; }
        public Builder canUse(boolean canUse) { this.canUse = canUse; return this; }
        public Builder canManage(boolean canManage) { this.canManage = canManage; return this; }
        public Builder canAssign(boolean canAssign) { this.canAssign = canAssign; return this; }
        public Builder canInspect(boolean canInspect) { this.canInspect = canInspect; return this; }
        public Builder useSchedule(String useSchedule) { this.useSchedule = useSchedule; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder allocatedCapacity(Integer allocatedCapacity) { this.allocatedCapacity = allocatedCapacity; return this; }
        public Builder weightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }

        public SpaceOrgRelation build() {
            return new SpaceOrgRelation(this);
        }
    }
}
