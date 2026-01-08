package com.school.management.dto.task;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务统计DTO
 */
@Data
public class TaskStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总任务数
     */
    private Long totalCount = 0L;

    /**
     * 待接收数量
     */
    private Long pendingCount = 0L;

    /**
     * 进行中数量
     */
    private Long inProgressCount = 0L;

    /**
     * 待审核数量
     */
    private Long submittedCount = 0L;

    /**
     * 已完成数量
     */
    private Long completedCount = 0L;

    /**
     * 已打回数量
     */
    private Long rejectedCount = 0L;

    /**
     * 已取消数量
     */
    private Long cancelledCount = 0L;

    /**
     * 已超期数量
     */
    private Long overdueCount = 0L;

    /**
     * 完成率(%)
     */
    private Double completionRate = 0.0;

    /**
     * 超期率(%)
     */
    private Double overdueRate = 0.0;

    /**
     * 待我审批数量
     */
    private Long pendingApprovalCount = 0L;
}
