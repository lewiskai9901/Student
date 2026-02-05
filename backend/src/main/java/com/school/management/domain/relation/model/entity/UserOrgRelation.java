package com.school.management.domain.relation.model.entity;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用户-组织关系领域实体
 * 支持用户多组织归属：主归属、副职、临时归属等
 */
public class UserOrgRelation implements Entity<Long> {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 关系类型
     */
    private RelationType relationType;

    /**
     * 职务名称
     */
    private String positionTitle;

    /**
     * 职务级别
     */
    private Integer positionLevel;

    /**
     * 是否主归属
     */
    private boolean isPrimary;

    /**
     * 是否领导
     */
    private boolean isLeader;

    /**
     * 是否有管理权限
     */
    private boolean canManage;

    /**
     * 是否有审批权限
     */
    private boolean canApprove;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 权重比例
     */
    private BigDecimal weightRatio;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private Long createdBy;

    public UserOrgRelation() {}

    public UserOrgRelation(Long id, Long userId, Long orgUnitId, RelationType relationType,
                           String positionTitle, Integer positionLevel, boolean isPrimary,
                           boolean isLeader, boolean canManage, boolean canApprove,
                           LocalDate startDate, LocalDate endDate, BigDecimal weightRatio,
                           Integer sortOrder, String remark, Long createdBy) {
        this.id = id;
        this.userId = userId;
        this.orgUnitId = orgUnitId;
        this.relationType = relationType;
        this.positionTitle = positionTitle;
        this.positionLevel = positionLevel;
        this.isPrimary = isPrimary;
        this.isLeader = isLeader;
        this.canManage = canManage;
        this.canApprove = canApprove;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weightRatio = weightRatio;
        this.sortOrder = sortOrder;
        this.remark = remark;
        this.createdBy = createdBy;
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public RelationType getRelationType() { return relationType; }
    public String getPositionTitle() { return positionTitle; }
    public Integer getPositionLevel() { return positionLevel; }
    public boolean isPrimary() { return isPrimary; }
    public boolean isLeader() { return isLeader; }
    public boolean isCanManage() { return canManage; }
    public boolean isCanApprove() { return canApprove; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getWeightRatio() { return weightRatio; }
    public Integer getSortOrder() { return sortOrder; }
    public String getRemark() { return remark; }
    public Long getCreatedBy() { return createdBy; }

    // Builder
    public static UserOrgRelationBuilder builder() { return new UserOrgRelationBuilder(); }

    public static class UserOrgRelationBuilder {
        private Long id;
        private Long userId;
        private Long orgUnitId;
        private RelationType relationType;
        private String positionTitle;
        private Integer positionLevel;
        private boolean isPrimary;
        private boolean isLeader;
        private boolean canManage;
        private boolean canApprove;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;
        private Long createdBy;

        public UserOrgRelationBuilder id(Long id) { this.id = id; return this; }
        public UserOrgRelationBuilder userId(Long userId) { this.userId = userId; return this; }
        public UserOrgRelationBuilder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public UserOrgRelationBuilder relationType(RelationType relationType) { this.relationType = relationType; return this; }
        public UserOrgRelationBuilder positionTitle(String positionTitle) { this.positionTitle = positionTitle; return this; }
        public UserOrgRelationBuilder positionLevel(Integer positionLevel) { this.positionLevel = positionLevel; return this; }
        public UserOrgRelationBuilder isPrimary(boolean isPrimary) { this.isPrimary = isPrimary; return this; }
        public UserOrgRelationBuilder isLeader(boolean isLeader) { this.isLeader = isLeader; return this; }
        public UserOrgRelationBuilder canManage(boolean canManage) { this.canManage = canManage; return this; }
        public UserOrgRelationBuilder canApprove(boolean canApprove) { this.canApprove = canApprove; return this; }
        public UserOrgRelationBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public UserOrgRelationBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public UserOrgRelationBuilder weightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; return this; }
        public UserOrgRelationBuilder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public UserOrgRelationBuilder remark(String remark) { this.remark = remark; return this; }
        public UserOrgRelationBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }

        public UserOrgRelation build() {
            return new UserOrgRelation(id, userId, orgUnitId, relationType, positionTitle, positionLevel,
                    isPrimary, isLeader, canManage, canApprove, startDate, endDate, weightRatio, sortOrder, remark, createdBy);
        }
    }

    /**
     * 关系类型枚举
     */
    public enum RelationType {
        /**
         * 主归属
         */
        PRIMARY("主归属"),
        /**
         * 副职/兼职
         */
        SECONDARY("副职"),
        /**
         * 临时借调
         */
        TEMPORARY("临时借调"),
        /**
         * 分管
         */
        SUPERVISING("分管");

        private final String description;

        RelationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public String getLabel() {
            return description;
        }
    }

    /**
     * 更新职务信息
     */
    public void updatePosition(String positionTitle, Integer positionLevel, boolean isLeader) {
        this.positionTitle = positionTitle;
        this.positionLevel = positionLevel;
        this.isLeader = isLeader;
    }

    /**
     * 更新权限配置
     */
    public void updatePermissions(boolean canManage, boolean canApprove) {
        this.canManage = canManage;
        this.canApprove = canApprove;
    }

    /**
     * 更新有效期
     */
    public void updatePeriod(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * 更新权重
     */
    public void updateWeight(BigDecimal weightRatio) {
        this.weightRatio = weightRatio;
    }

    /**
     * 更新排序
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设为主归属
     */
    public void setAsPrimary() {
        this.isPrimary = true;
        this.relationType = RelationType.PRIMARY;
    }

    /**
     * 取消主归属
     */
    public void clearPrimary() {
        this.isPrimary = false;
    }

    /**
     * 是否有效（临时归属检查有效期）
     */
    public boolean isActive() {
        if (relationType != RelationType.TEMPORARY) {
            return true;
        }
        LocalDate today = LocalDate.now();
        boolean afterStart = startDate == null || !today.isBefore(startDate);
        boolean beforeEnd = endDate == null || !today.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        if (endDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(endDate);
    }

    /**
     * 是否即将过期（7天内）
     */
    public boolean isExpiringSoon() {
        return isExpiringSoon(7);
    }

    /**
     * 是否即将过期（指定天数内）
     */
    public boolean isExpiringSoon(int days) {
        if (endDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !today.isAfter(endDate) && today.plusDays(days).isAfter(endDate);
    }
}
