package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建调课申请命令
 */
@Data
@Builder
public class CreateAdjustmentCommand {

    @NotNull(message = "课表条目ID不能为空")
    private Long entryId;

    /**
     * 调整类型: 1-调课 2-停课 3-补课 4-代课
     */
    @NotNull(message = "调整类型不能为空")
    private Integer adjustType;

    /**
     * 原日期
     */
    @NotNull(message = "原日期不能为空")
    private LocalDate originalDate;

    /**
     * 原节次
     */
    @NotNull(message = "原节次不能为空")
    private Integer originalSlot;

    /**
     * 新日期(停课时可为空)
     */
    private LocalDate newDate;

    /**
     * 新节次
     */
    private Integer newSlot;

    /**
     * 新教室ID
     */
    private Long newClassroomId;

    /**
     * 代课教师ID(代课时必填)
     */
    private Long substituteTeacherId;

    /**
     * 调课原因
     */
    private String reason;

    /**
     * 操作人
     */
    private Long operatorId;
}
