package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建调课申请请求
 */
@Data
public class CreateAdjustmentRequest {

    @NotNull(message = "课表条目ID不能为空")
    private Long entryId;

    /**
     * 调整类型: 1-调课 2-停课 3-补课 4-代课
     */
    @NotNull(message = "调整类型不能为空")
    private Integer adjustType;

    @NotNull(message = "原日期不能为空")
    private LocalDate originalDate;

    @NotNull(message = "原节次不能为空")
    private Integer originalSlot;

    private LocalDate newDate;

    private Integer newSlot;

    private Long newClassroomId;

    private Long substituteTeacherId;

    private String reason;
}
