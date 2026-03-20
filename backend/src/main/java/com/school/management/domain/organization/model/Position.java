package com.school.management.domain.organization.model;

import com.school.management.domain.organization.event.PositionCreatedEvent;
import com.school.management.domain.organization.event.PositionUpdatedEvent;
import com.school.management.domain.shared.model.valueobject.FieldChange;
import com.school.management.domain.organization.model.valueobject.JobLevel;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 岗位聚合根
 */
public class Position extends AggregateRoot<Long> {

    private Long id;
    private String positionCode;
    private String positionName;
    private Long orgUnitId;
    private String jobLevel;
    private int headcount;
    private Long reportsToId;
    private String responsibilities;
    private String requirements;
    private int sortOrder;
    private boolean keyPosition;
    private boolean enabled;
    private Long tenantId;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    protected Position() {
    }

    public static Position create(String positionCode, String positionName, Long orgUnitId,
                                   int headcount, Long createdBy) {
        Position p = new Position();
        p.positionCode = Objects.requireNonNull(positionCode);
        p.positionName = Objects.requireNonNull(positionName);
        p.orgUnitId = Objects.requireNonNull(orgUnitId);
        p.headcount = headcount > 0 ? headcount : 1;
        p.enabled = true;
        p.sortOrder = 0;
        p.setVersion(0L);
        p.tenantId = 1L;
        p.createdBy = createdBy;
        p.createdAt = LocalDateTime.now();
        p.updatedAt = p.createdAt;

        p.registerEvent(new PositionCreatedEvent(p));
        return p;
    }

    /**
     * 从持久化数据重建
     */
    public static Position reconstruct(Long id, String positionCode, String positionName,
                                        Long orgUnitId, String jobLevel, int headcount,
                                        Long reportsToId, String responsibilities,
                                        String requirements, int sortOrder,
                                        boolean keyPosition, boolean enabled, Long version,
                                        Long tenantId, LocalDateTime createdAt, Long createdBy,
                                        LocalDateTime updatedAt, Long updatedBy) {
        Position p = new Position();
        p.id = id;
        p.positionCode = positionCode;
        p.positionName = positionName;
        p.orgUnitId = orgUnitId;
        p.jobLevel = jobLevel;
        p.headcount = headcount;
        p.reportsToId = reportsToId;
        p.responsibilities = responsibilities;
        p.requirements = requirements;
        p.sortOrder = sortOrder;
        p.keyPosition = keyPosition;
        p.enabled = enabled;
        p.setVersion(version);
        p.tenantId = tenantId;
        p.createdAt = createdAt;
        p.createdBy = createdBy;
        p.updatedAt = updatedAt;
        p.updatedBy = updatedBy;
        return p;
    }

    /**
     * 更新岗位，返回变更字段列表
     */
    public List<FieldChange> update(String positionName, String jobLevel, int headcount,
                                     Long reportsToId, String responsibilities,
                                     String requirements, boolean keyPosition,
                                     int sortOrder, Long updatedBy) {
        List<FieldChange> changes = new ArrayList<>();

        if (positionName != null && !positionName.equals(this.positionName)) {
            changes.add(new FieldChange("positionName", this.positionName, positionName));
            this.positionName = positionName;
        }
        if (!Objects.equals(jobLevel, this.jobLevel)) {
            changes.add(new FieldChange("jobLevel", this.jobLevel, jobLevel));
            this.jobLevel = jobLevel;
        }
        if (headcount != this.headcount) {
            changes.add(new FieldChange("headcount", String.valueOf(this.headcount), String.valueOf(headcount)));
            this.headcount = headcount;
        }
        if (!Objects.equals(reportsToId, this.reportsToId)) {
            changes.add(new FieldChange("reportsToId",
                this.reportsToId != null ? this.reportsToId.toString() : null,
                reportsToId != null ? reportsToId.toString() : null));
            this.reportsToId = reportsToId;
        }
        if (!Objects.equals(responsibilities, this.responsibilities)) {
            changes.add(new FieldChange("responsibilities", this.responsibilities, responsibilities));
            this.responsibilities = responsibilities;
        }
        if (!Objects.equals(requirements, this.requirements)) {
            changes.add(new FieldChange("requirements", this.requirements, requirements));
            this.requirements = requirements;
        }
        if (keyPosition != this.keyPosition) {
            changes.add(new FieldChange("keyPosition", String.valueOf(this.keyPosition), String.valueOf(keyPosition)));
            this.keyPosition = keyPosition;
        }
        if (sortOrder != this.sortOrder) {
            changes.add(new FieldChange("sortOrder", String.valueOf(this.sortOrder), String.valueOf(sortOrder)));
            this.sortOrder = sortOrder;
        }

        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        if (!changes.isEmpty()) {
            registerEvent(new PositionUpdatedEvent(this, changes));
        }
        return changes;
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isVacant(int currentCount) {
        return headcount > currentCount;
    }

    public boolean isOverstaffed(int currentCount) {
        return currentCount > headcount;
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPositionCode() { return positionCode; }
    public String getPositionName() { return positionName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getJobLevel() { return jobLevel; }
    public void setJobLevel(String jobLevel) { this.jobLevel = jobLevel; }
    public int getHeadcount() { return headcount; }
    public Long getReportsToId() { return reportsToId; }
    public void setReportsToId(Long reportsToId) { this.reportsToId = reportsToId; }
    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public int getSortOrder() { return sortOrder; }
    public boolean isKeyPosition() { return keyPosition; }
    public boolean isEnabled() { return enabled; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
}
