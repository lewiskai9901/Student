package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * 创建课表条目命令
 */
@Data
@Builder
public class CreateScheduleEntryCommand {

    @NotNull(message = "教学任务ID不能为空")
    private Long taskId;

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotNull(message = "星期不能为空")
    private Integer weekday;

    @NotNull(message = "节次不能为空")
    private Integer slot;

    /**
     * 起始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 周类型: 0-每周 1-单周 2-双周
     */
    private Integer weekType;

    /**
     * 教室ID
     */
    private Long classroomId;

    /**
     * 操作人
     */
    private Long operatorId;
}
