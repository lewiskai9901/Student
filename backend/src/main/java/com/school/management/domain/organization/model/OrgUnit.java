package com.school.management.domain.organization.model;

import com.school.management.domain.organization.event.OrgUnitCreatedEvent;
import com.school.management.domain.organization.event.OrgUnitUpdatedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * OrgUnit Aggregate Root.
 * Represents an organizational unit in the tree structure.
 * Supports both academic units (school -> department -> major -> class)
 * and functional departments (student_affairs, academic_affairs, etc.)
 */
public class OrgUnit extends AggregateRoot<Long> {

    private Long id;
    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private UnitCategory unitCategory;  // 组织类别: ACADEMIC, FUNCTIONAL, ADMINISTRATIVE
    private Long parentId;
    private String treePath;
    private Integer treeLevel;
    private Long leaderId;
    private List<Long> deputyLeaderIds;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    // For JPA/MyBatis
    protected OrgUnit() {
    }

    private OrgUnit(Builder builder) {
        this.id = builder.id;
        this.unitCode = Objects.requireNonNull(builder.unitCode, "unitCode cannot be null");
        this.unitName = Objects.requireNonNull(builder.unitName, "unitName cannot be null");
        this.unitType = Objects.requireNonNull(builder.unitType, "unitType cannot be null");
        this.unitCategory = builder.unitCategory != null ? builder.unitCategory : UnitCategory.ACADEMIC;
        this.parentId = builder.parentId;
        this.treePath = builder.treePath;
        this.treeLevel = builder.treeLevel != null ? builder.treeLevel : 1;
        this.leaderId = builder.leaderId;
        this.deputyLeaderIds = builder.deputyLeaderIds != null
            ? new ArrayList<>(builder.deputyLeaderIds)
            : new ArrayList<>();
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.enabled = builder.enabled != null ? builder.enabled : true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = builder.createdBy;

        validate();
    }

    /**
     * Factory method to create a new OrgUnit.
     */
    public static OrgUnit create(String unitCode, String unitName, OrgUnitType unitType,
                                  Long parentId, Long createdBy) {
        OrgUnit orgUnit = builder()
            .unitCode(unitCode)
            .unitName(unitName)
            .unitType(unitType)
            .parentId(parentId)
            .createdBy(createdBy)
            .build();

        orgUnit.registerEvent(new OrgUnitCreatedEvent(orgUnit));
        return orgUnit;
    }

    /**
     * Updates the organization unit information.
     */
    public void update(String unitName, UnitCategory unitCategory, Long leaderId, List<Long> deputyLeaderIds,
                       Integer sortOrder, Long updatedBy) {
        if (unitName != null && !unitName.isBlank()) {
            this.unitName = unitName;
        }
        if (unitCategory != null) {
            this.unitCategory = unitCategory;
        }
        this.leaderId = leaderId;
        this.deputyLeaderIds = deputyLeaderIds != null
            ? new ArrayList<>(deputyLeaderIds)
            : new ArrayList<>();
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new OrgUnitUpdatedEvent(this));
    }

    /**
     * Sets the tree path and level based on parent.
     */
    public void setTreePosition(String parentTreePath, int parentLevel) {
        if (parentTreePath == null || parentTreePath.isEmpty()) {
            this.treePath = "/" + this.id + "/";
            this.treeLevel = 1;
        } else {
            this.treePath = parentTreePath + this.id + "/";
            this.treeLevel = parentLevel + 1;
        }
    }

    /**
     * Enables the organization unit.
     */
    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Disables the organization unit.
     */
    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Assigns a leader to this organization unit.
     */
    public void assignLeader(Long leaderId, Long updatedBy) {
        this.leaderId = leaderId;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this unit is an ancestor of another unit.
     */
    public boolean isAncestorOf(OrgUnit other) {
        if (other == null || other.getTreePath() == null) {
            return false;
        }
        return other.getTreePath().startsWith(this.treePath)
            && !other.getId().equals(this.id);
    }

    /**
     * Checks if this unit is a descendant of another unit.
     */
    public boolean isDescendantOf(OrgUnit other) {
        if (other == null || this.treePath == null) {
            return false;
        }
        return this.treePath.startsWith(other.getTreePath())
            && !this.id.equals(other.getId());
    }

    private void validate() {
        if (unitCode == null || unitCode.isBlank()) {
            throw new IllegalArgumentException("Unit code cannot be empty");
        }
        if (unitCode.length() > 50) {
            throw new IllegalArgumentException("Unit code cannot exceed 50 characters");
        }
        if (unitName == null || unitName.isBlank()) {
            throw new IllegalArgumentException("Unit name cannot be empty");
        }
        if (unitName.length() > 100) {
            throw new IllegalArgumentException("Unit name cannot exceed 100 characters");
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

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public OrgUnitType getUnitType() {
        return unitType;
    }

    public UnitCategory getUnitCategory() {
        return unitCategory;
    }

    /**
     * Checks if this is an academic unit (school, department, major, class, etc.)
     */
    public boolean isAcademic() {
        return unitCategory == UnitCategory.ACADEMIC;
    }

    /**
     * Checks if this is a functional department (student_affairs, academic_affairs, etc.)
     */
    public boolean isFunctional() {
        return unitCategory == UnitCategory.FUNCTIONAL;
    }

    /**
     * Checks if this is an administrative unit
     */
    public boolean isAdministrative() {
        return unitCategory == UnitCategory.ADMINISTRATIVE;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getTreePath() {
        return treePath;
    }

    public Integer getTreeLevel() {
        return treeLevel;
    }

    public Long getLeaderId() {
        return leaderId;
    }

    public List<Long> getDeputyLeaderIds() {
        return Collections.unmodifiableList(deputyLeaderIds);
    }

    public Integer getSortOrder() {
        return sortOrder;
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
        private String unitCode;
        private String unitName;
        private OrgUnitType unitType;
        private UnitCategory unitCategory;
        private Long parentId;
        private String treePath;
        private Integer treeLevel;
        private Long leaderId;
        private List<Long> deputyLeaderIds;
        private Integer sortOrder;
        private Boolean enabled;
        private Long createdBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder unitCode(String unitCode) {
            this.unitCode = unitCode;
            return this;
        }

        public Builder unitName(String unitName) {
            this.unitName = unitName;
            return this;
        }

        public Builder unitType(OrgUnitType unitType) {
            this.unitType = unitType;
            return this;
        }

        public Builder unitCategory(UnitCategory unitCategory) {
            this.unitCategory = unitCategory;
            return this;
        }

        public Builder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder treePath(String treePath) {
            this.treePath = treePath;
            return this;
        }

        public Builder treeLevel(Integer treeLevel) {
            this.treeLevel = treeLevel;
            return this;
        }

        public Builder leaderId(Long leaderId) {
            this.leaderId = leaderId;
            return this;
        }

        public Builder deputyLeaderIds(List<Long> deputyLeaderIds) {
            this.deputyLeaderIds = deputyLeaderIds;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public OrgUnit build() {
            return new OrgUnit(this);
        }
    }
}
