package com.school.management.domain.place.model.entity;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * 场所-班级分配实体
 * 支持一个场所分配给多个班级的场景（如部门分配宿舍给多个班级）
 */
public class PlaceClassAssignment implements Entity<Long> {

    private Long id;
    private Long placeId;
    private Long classId;
    private Long orgUnitId;         // 所属部门ID（冗余，便于权限控制）
    private Integer assignedBeds;    // 分配床位数（NULL表示不限）
    private Integer priority;        // 优先级（数值越大越优先）
    private boolean enabled;         // 是否启用
    private String remark;
    private Long assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PlaceClassAssignment() {}

    // Getters
    @Override
    public Long getId() { return id; }
    public Long getPlaceId() { return placeId; }
    public Long getClassId() { return classId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Integer getAssignedBeds() { return assignedBeds; }
    public Integer getPriority() { return priority; }
    public boolean isEnabled() { return enabled; }
    public String getRemark() { return remark; }
    public Long getAssignedBy() { return assignedBy; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * 创建新的场所-班级分配
     */
    public static PlaceClassAssignment create(Long placeId, Long classId, Long orgUnitId,
                                               Integer assignedBeds, Long assignedBy) {
        PlaceClassAssignment assignment = new PlaceClassAssignment();
        assignment.placeId = placeId;
        assignment.classId = classId;
        assignment.orgUnitId = orgUnitId;
        assignment.assignedBeds = assignedBeds;
        assignment.priority = 0;
        assignment.enabled = true;
        assignment.assignedBy = assignedBy;
        assignment.assignedAt = LocalDateTime.now();
        assignment.createdAt = LocalDateTime.now();
        return assignment;
    }

    /**
     * 从持久化数据重建
     */
    public static PlaceClassAssignment reconstitute(Long id, Long placeId, Long classId, Long orgUnitId,
                                                     Integer assignedBeds, Integer priority, Integer status,
                                                     String remark, Long assignedBy, LocalDateTime assignedAt,
                                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        PlaceClassAssignment assignment = new PlaceClassAssignment();
        assignment.id = id;
        assignment.placeId = placeId;
        assignment.classId = classId;
        assignment.orgUnitId = orgUnitId;
        assignment.assignedBeds = assignedBeds;
        assignment.priority = priority != null ? priority : 0;
        assignment.enabled = status == null || status == 1;
        assignment.remark = remark;
        assignment.assignedBy = assignedBy;
        assignment.assignedAt = assignedAt;
        assignment.createdAt = createdAt;
        assignment.updatedAt = updatedAt;
        return assignment;
    }

    /**
     * 设置ID（保存后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 更新分配信息
     */
    public void update(Integer assignedBeds, Integer priority, String remark) {
        this.assignedBeds = assignedBeds;
        if (priority != null) {
            this.priority = priority;
        }
        this.remark = remark;
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
     * 设置优先级
     */
    public void setPriority(int priority) {
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取状态码（用于持久化）
     */
    public int getStatusCode() {
        return enabled ? 1 : 0;
    }
}
