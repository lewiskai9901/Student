package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Space-Organization Relation Persistence Object
 */
@TableName("space_org_relations")
public class SpaceOrgRelationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spaceId;
    private Long orgUnitId;
    private String relationType;
    private Boolean isPrimary;
    private Integer priorityLevel;
    private Boolean canUse;
    private Boolean canManage;
    private Boolean canAssign;
    private Boolean canInspect;
    private String useSchedule;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer allocatedCapacity;
    private BigDecimal weightRatio;
    private Integer sortOrder;
    private String remark;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // Getters
    public Long getId() { return id; }
    public Long getSpaceId() { return spaceId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getRelationType() { return relationType; }
    public Boolean getIsPrimary() { return isPrimary; }
    public Integer getPriorityLevel() { return priorityLevel; }
    public Boolean getCanUse() { return canUse; }
    public Boolean getCanManage() { return canManage; }
    public Boolean getCanAssign() { return canAssign; }
    public Boolean getCanInspect() { return canInspect; }
    public String getUseSchedule() { return useSchedule; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getAllocatedCapacity() { return allocatedCapacity; }
    public BigDecimal getWeightRatio() { return weightRatio; }
    public Integer getSortOrder() { return sortOrder; }
    public String getRemark() { return remark; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getDeleted() { return deleted; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
    public void setCanUse(Boolean canUse) { this.canUse = canUse; }
    public void setCanManage(Boolean canManage) { this.canManage = canManage; }
    public void setCanAssign(Boolean canAssign) { this.canAssign = canAssign; }
    public void setCanInspect(Boolean canInspect) { this.canInspect = canInspect; }
    public void setUseSchedule(String useSchedule) { this.useSchedule = useSchedule; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setAllocatedCapacity(Integer allocatedCapacity) { this.allocatedCapacity = allocatedCapacity; }
    public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
