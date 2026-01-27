package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建课表条目请求
 */
@Data
public class CreateScheduleEntryRequest {

    @NotNull(message = "教学任务ID不能为空")
    private Long taskId;

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotNull(message = "星期不能为空")
    @Min(value = 1, message = "星期必须在1-7之间")
    @Max(value = 7, message = "星期必须在1-7之间")
    private Integer weekday;

    @NotNull(message = "节次不能为空")
    @Min(value = 1, message = "节次必须大于0")
    private Integer slot;

    private Integer startWeek;

    private Integer endWeek;

    /**
     * 周类型: 0-每周 1-单周 2-双周
     */
    private Integer weekType;

    private Long classroomId;
}
