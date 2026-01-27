package com.school.management.dto.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务查询请求DTO
 */
@Data
public class TaskQueryRequest {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 关键词(标题/任务编号)
     */
    private String keyword;

    /**
     * 任务状态
     */
    private Integer status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 分配人ID
     */
    private Long assignerId;

    /**
     * 执行人ID
     */
    private Long assigneeId;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 创建时间-开始
     */
    private LocalDateTime startTime;

    /**
     * 创建时间-结束
     */
    private LocalDateTime endTime;

    /**
     * 截止时间-开始
     */
    private LocalDateTime dueStartTime;

    /**
     * 截止时间-结束
     */
    private LocalDateTime dueEndTime;

    /**
     * 是否只查询我的任务
     */
    private Boolean myTask = false;

    /**
     * 是否只查询待我审批
     */
    private Boolean pendingApproval = false;
}
