package com.school.management.domain.relation.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用户-场所关系实体
 * 支持用户的场所分配（如学生宿舍床位、教师办公室等）
 */
@Getter
@Builder
public class UserSpaceRelation implements Entity<Long> {

    private Long id;
    private Long userId;
    private Long spaceId;
    private RelationType relationType;

    // 分配信息
    private String positionCode;
    private String positionName;

    // 归属属性
    private boolean isPrimary;

    // 使用权限
    private boolean canUse;
    private boolean canManage;

    // 有效期
    private LocalDate startDate;
    private LocalDate endDate;

    // 费用相关
    private BigDecimal feeAmount;
    private boolean feePaid;

    // 排序和备注
    private Integer sortOrder;
    private String remark;

    // 审计
    private Long createdBy;

    /**
     * 关系类型枚举
     */
    public enum RelationType {
        ASSIGNED("分配"),
        MANAGED("管理"),
        TEMPORARY("临时");

        private final String label;

        RelationType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 更新位置信息
     */
    public void updatePosition(String positionCode, String positionName) {
        this.positionCode = positionCode;
        this.positionName = positionName;
    }

    /**
     * 更新权限
     */
    public void updatePermissions(boolean canUse, boolean canManage) {
        this.canUse = canUse;
        this.canManage = canManage;
    }

    /**
     * 更新有效期
     */
    public void updatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * 更新费用信息
     */
    public void updateFee(BigDecimal feeAmount, boolean feePaid) {
        if (feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("费用金额不能为负数");
        }
        this.feeAmount = feeAmount;
        this.feePaid = feePaid;
    }

    /**
     * 标记已缴费
     */
    public void markAsPaid() {
        this.feePaid = true;
    }

    /**
     * 更新排序
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    /**
     * 设为主要场所
     */
    public void setAsPrimary() {
        this.isPrimary = true;
    }

    /**
     * 取消主要场所
     */
    public void clearPrimary() {
        this.isPrimary = false;
    }

    /**
     * 检查关系是否有效
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        boolean afterStart = startDate == null || !today.isBefore(startDate);
        boolean beforeEnd = endDate == null || !today.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return endDate != null && LocalDate.now().isAfter(endDate);
    }

    /**
     * 检查是否即将过期（默认7天内）
     */
    public boolean isExpiringSoon(int days) {
        if (endDate == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !isExpired() && !endDate.isAfter(warningDate);
    }

    /**
     * 是否需要缴费
     */
    public boolean needsPayment() {
        return feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) > 0 && !feePaid;
    }
}
