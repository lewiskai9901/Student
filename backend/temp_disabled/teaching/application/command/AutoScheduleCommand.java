package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 自动排课命令
 */
@Data
@Builder
public class AutoScheduleCommand {

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    /**
     * 要排课的教学任务ID列表(为空则排所有未排课任务)
     */
    private List<Long> taskIds;

    /**
     * 最大迭代次数
     */
    private Integer maxIterations;

    /**
     * 种群大小
     */
    private Integer populationSize;

    /**
     * 变异率
     */
    private Double mutationRate;

    /**
     * 是否允许覆盖现有课表
     */
    private Boolean allowOverwrite;

    /**
     * 操作人
     */
    private Long operatorId;
}
