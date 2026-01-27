package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调课记录持久化对象
 */
@Data
@TableName("schedule_adjustments")
public class ScheduleAdjustmentPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long entryId;

    /**
     * 调整类型: 1-调课 2-停课 3-补课 4-代课
     */
    private Integer adjustType;

    private LocalDate originalDate;

    private Integer originalSlot;

    private LocalDate newDate;

    private Integer newSlot;

    private Long newClassroomId;

    private Long substituteTeacherId;

    private String reason;

    /**
     * 状态: 0-待审批 1-已批准 2-已拒绝 3-已取消
     */
    private Integer status;

    private Long applicantId;

    private Long approverId;

    private LocalDateTime approvedAt;

    private String approvalRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
