package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 自动排课请求
 */
@Data
public class AutoScheduleRequest {

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    /**
     * 要排课的教学任务ID列表(为空则排所有未排课任务)
     */
    private List<Long> taskIds;

    /**
     * 最大迭代次数(默认1000)
     */
    private Integer maxIterations;

    /**
     * 种群大小(默认100)
     */
    private Integer populationSize;

    /**
     * 变异率(默认0.1)
     */
    private Double mutationRate;

    /**
     * 是否允许覆盖现有课表
     */
    private Boolean allowOverwrite;
}
