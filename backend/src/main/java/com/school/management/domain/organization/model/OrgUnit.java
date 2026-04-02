package com.school.management.domain.organization.model;

import com.school.management.domain.organization.event.OrgUnitCreatedEvent;
import com.school.management.domain.organization.event.OrgUnitUpdatedEvent;
import com.school.management.domain.shared.model.valueobject.FieldChange;
import com.school.management.domain.organization.model.valueobject.OrgUnitStatus;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OrgUnit Aggregate Root.
 * Represents an organizational unit in the tree structure.
 * unitType is a String (typeCode) driven by org_unit_types database configuration.
 */
public class OrgUnit extends AggregateRoot<Long> {

    private Long id;
    private String unitCode;
    private String unitName;
    private String unitType;           // typeCode from org_unit_types table
    private Long parentId;
    private String treePath;
    private Integer treeLevel;
    private Integer sortOrder;
    private OrgUnitStatus status;
    private Integer headcount;
    private Map<String, Object> attributes;
    private Long mergedIntoId;
    private Long splitFromId;
    private LocalDateTime dissolvedAt;
    private String dissolvedReason;
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
        if (this.unitType.isBlank()) {
            throw new IllegalArgumentException("unitType cannot be blank");
        }
        this.parentId = builder.parentId;
        this.treePath = builder.treePath;
        this.treeLevel = builder.treeLevel != null ? builder.treeLevel : 1;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.status = builder.status != null ? builder.status : OrgUnitStatus.ACTIVE;
        this.headcount = builder.headcount;
        this.attributes = builder.attributes != null ? new HashMap<>(builder.attributes) : null;
        this.mergedIntoId = builder.mergedIntoId;
        this.splitFromId = builder.splitFromId;
        this.dissolvedAt = builder.dissolvedAt;
        this.dissolvedReason = builder.dissolvedReason;
        setVersion(builder.version);
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;

        validate();
    }

    /**
     * Factory method to create a new OrgUnit.
     */
    public static OrgUnit create(String unitCode, String unitName, String unitType,
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
     * Returns list of FieldChange for change logging.
     */
    public List<FieldChange> update(String unitName, Integer sortOrder, Integer headcount,
                                     Map<String, Object> attributes, Long updatedBy) {
        List<FieldChange> changes = new ArrayList<>();

        if (unitName != null && !unitName.isBlank() && !unitName.equals(this.unitName)) {
            changes.add(new FieldChange("unitName", this.unitName, unitName));
            this.unitName = unitName;
        }
        if (sortOrder != null && !sortOrder.equals(this.sortOrder)) {
            changes.add(new FieldChange("sortOrder",
                this.sortOrder != null ? this.sortOrder.toString() : null,
                sortOrder.toString()));
            this.sortOrder = sortOrder;
        }
        if (!Objects.equals(headcount, this.headcount)) {
            changes.add(new FieldChange("headcount",
                this.headcount != null ? this.headcount.toString() : null,
                headcount != null ? headcount.toString() : null));
            this.headcount = headcount;
        }
        if (attributes != null && !attributes.equals(this.attributes)) {
            changes.add(new FieldChange("attributes",
                this.attributes != null ? this.attributes.toString() : null,
                attributes.toString()));
            this.attributes = new HashMap<>(attributes);
        }

        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new OrgUnitUpdatedEvent(this));
        return changes;
    }

    // ==================== Lifecycle Operations ====================

    /**
     * Freeze the org unit — no new positions/members can be added.
     */
    public List<FieldChange> freeze(String reason, Long updatedBy) {
        validateStatusTransition(OrgUnitStatus.FROZEN);
        List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange("status", this.status.name(), OrgUnitStatus.FROZEN.name()));
        this.status = OrgUnitStatus.FROZEN;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        return changes;
    }

    /**
     * Unfreeze the org unit — restore to ACTIVE.
     */
    public List<FieldChange> unfreeze(Long updatedBy) {
        validateStatusTransition(OrgUnitStatus.ACTIVE);
        List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange("status", this.status.name(), OrgUnitStatus.ACTIVE.name()));
        this.status = OrgUnitStatus.ACTIVE;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        return changes;
    }

    /**
     * Dissolve the org unit.
     */
    public List<FieldChange> dissolve(String reason, Long updatedBy) {
        validateStatusTransition(OrgUnitStatus.DISSOLVED);
        List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange("status", this.status.name(), OrgUnitStatus.DISSOLVED.name()));
        this.status = OrgUnitStatus.DISSOLVED;
        this.dissolvedAt = LocalDateTime.now();
        this.dissolvedReason = reason;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        return changes;
    }

    /**
     * Mark as merging into target org unit.
     */
    public void markMergedInto(Long targetId, String reason, Long updatedBy) {
        validateStatusTransition(OrgUnitStatus.DISSOLVED);
        this.status = OrgUnitStatus.DISSOLVED;
        this.mergedIntoId = targetId;
        this.dissolvedAt = LocalDateTime.now();
        this.dissolvedReason = reason;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as split from source org unit.
     */
    public void markSplitFrom(Long sourceId) {
        this.splitFromId = sourceId;
    }

    /**
     * Move to new parent.
     */
    public void moveToParent(Long newParentId, Long updatedBy) {
        this.parentId = newParentId;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Status Queries ====================

    public boolean canAddPositions() {
        return status == OrgUnitStatus.ACTIVE;
    }

    public boolean canAddChildren() {
        return status == OrgUnitStatus.ACTIVE || status == OrgUnitStatus.DRAFT;
    }

    // ==================== Status Transition Validation ====================

    /**
     * Validates that a status transition is allowed.
     * Allowed transitions:
     *   DRAFT    → ACTIVE, DISSOLVED
     *   ACTIVE   → FROZEN, DISSOLVED
     *   FROZEN   → ACTIVE, DISSOLVED
     *   DISSOLVED → (terminal, no transitions)
     *   MERGING  → DISSOLVED
     */
    public void validateStatusTransition(OrgUnitStatus newStatus) {
        if (this.status == newStatus) {
            return; // no-op
        }
        boolean allowed = switch (this.status) {
            case DRAFT -> newStatus == OrgUnitStatus.ACTIVE || newStatus == OrgUnitStatus.DISSOLVED;
            case ACTIVE -> newStatus == OrgUnitStatus.FROZEN || newStatus == OrgUnitStatus.DISSOLVED;
            case FROZEN -> newStatus == OrgUnitStatus.ACTIVE || newStatus == OrgUnitStatus.DISSOLVED;
            case MERGING -> newStatus == OrgUnitStatus.DISSOLVED;
            case DISSOLVED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this.status, newStatus));
        }
    }

    public boolean isActive() {
        return status == OrgUnitStatus.ACTIVE;
    }

    public boolean isDissolved() {
        return status == OrgUnitStatus.DISSOLVED;
    }

    // ==================== Tree Operations ====================

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

    public boolean isAncestorOf(OrgUnit other) {
        if (other == null || other.getTreePath() == null || this.treePath == null) {
            return false;
        }
        return other.getTreePath().startsWith(this.treePath)
            && !other.getId().equals(this.id);
    }

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

    public String getUnitCode() { return unitCode; }
    public String getUnitName() { return unitName; }
    public String getUnitType() { return unitType; }
    public Long getParentId() { return parentId; }
    public String getTreePath() { return treePath; }
    public Integer getTreeLevel() { return treeLevel; }
    public Integer getSortOrder() { return sortOrder; }
    public OrgUnitStatus getStatus() { return status; }
    public Integer getHeadcount() { return headcount; }
    public Map<String, Object> getAttributes() { return attributes; }
    public Long getMergedIntoId() { return mergedIntoId; }
    public Long getSplitFromId() { return splitFromId; }
    public LocalDateTime getDissolvedAt() { return dissolvedAt; }
    public String getDissolvedReason() { return dissolvedReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }

    /** @deprecated Use getStatus() == OrgUnitStatus.ACTIVE instead */
    public boolean isEnabled() {
        return status == OrgUnitStatus.ACTIVE || status == OrgUnitStatus.DRAFT;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String unitCode;
        private String unitName;
        private String unitType;
        private Long parentId;
        private String treePath;
        private Integer treeLevel;
        private Integer sortOrder;
        private OrgUnitStatus status;
        private Integer headcount;
        private Map<String, Object> attributes;
        private Long mergedIntoId;
        private Long splitFromId;
        private LocalDateTime dissolvedAt;
        private String dissolvedReason;
        private Long version;
        private Long createdBy;
        private Long updatedBy;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder unitCode(String unitCode) { this.unitCode = unitCode; return this; }
        public Builder unitName(String unitName) { this.unitName = unitName; return this; }
        public Builder unitType(String unitType) { this.unitType = unitType; return this; }
        public Builder parentId(Long parentId) { this.parentId = parentId; return this; }
        public Builder treePath(String treePath) { this.treePath = treePath; return this; }
        public Builder treeLevel(Integer treeLevel) { this.treeLevel = treeLevel; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder status(OrgUnitStatus status) { this.status = status; return this; }
        public Builder headcount(Integer headcount) { this.headcount = headcount; return this; }
        public Builder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }
        public Builder mergedIntoId(Long mergedIntoId) { this.mergedIntoId = mergedIntoId; return this; }
        public Builder splitFromId(Long splitFromId) { this.splitFromId = splitFromId; return this; }
        public Builder dissolvedAt(LocalDateTime dissolvedAt) { this.dissolvedAt = dissolvedAt; return this; }
        public Builder dissolvedReason(String dissolvedReason) { this.dissolvedReason = dissolvedReason; return this; }
        public Builder version(Long version) { this.version = version; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        /** @deprecated Use status(OrgUnitStatus) instead */
        public Builder enabled(Boolean enabled) {
            if (enabled != null) {
                this.status = enabled ? OrgUnitStatus.ACTIVE : OrgUnitStatus.FROZEN;
            }
            return this;
        }

        public OrgUnit build() {
            return new OrgUnit(this);
        }
    }
}
