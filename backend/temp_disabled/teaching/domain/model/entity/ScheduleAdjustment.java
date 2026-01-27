package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调课申请实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAdjustment {

    private Long id;

    /**
     * 原排课条目ID
     */
    private Long entryId;

    /**
     * 调整类型：1调课（换时间）2换教室 3补课 4代课
     */
    private Integer adjustType;

    // 原始信息
    private LocalDate originalDate;
    private Integer originalSlot;
    private Long originalClassroomId;

    // 调整后信息
    private LocalDate newDate;
    private Integer newSlot;
    private Long newClassroomId;
    private Long substituteTeacherId;

    // 申请信息
    private Long applicantId;
    private String reason;
    private LocalDateTime createdAt;

    // 审批信息
    /**
     * 审批状态：0待审批 1已通过 2已拒绝 3已取消
     */
    @Builder.Default
    private Integer status = 0;
    private Long approverId;
    private LocalDateTime approvedAt;
    private String approvalRemark;

    private LocalDateTime updatedAt;

    /**
     * 获取调整类型名称
     */
    public String getAdjustTypeName() {
        if (adjustType == null) return "未知";
        return switch (adjustType) {
            case 1 -> "调课";
            case 2 -> "换教室";
            case 3 -> "补课";
            case 4 -> "代课";
            default -> "未知";
        };
    }

    /**
     * 获取审批状态名称
     */
    public String getStatusName() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审批";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    /**
     * 是否可编辑
     */
    public boolean isEditable() {
        return status == null || status == 0;
    }

    /**
     * 是否已通过
     */
    public boolean isApproved() {
        return status != null && status == 1;
    }

    /**
     * 是否待审批
     */
    public boolean isPending() {
        return status == null || status == 0;
    }
}
