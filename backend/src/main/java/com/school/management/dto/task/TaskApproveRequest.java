package com.school.management.dto.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任务审批请求DTO
 */
@Data
public class TaskApproveRequest {

    /**
     * 任务ID
     */
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    /**
     * 提交记录ID
     */
    @NotNull(message = "提交记录ID不能为空")
    private Long submissionId;

    /**
     * 审批动作: 1-通过, 2-打回
     */
    @NotNull(message = "审批动作不能为空")
    @Min(value = 1, message = "审批动作必须是1(通过)或2(打回)")
    @Max(value = 2, message = "审批动作必须是1(通过)或2(打回)")
    private Integer action;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 打回到的节点(打回时使用)
     */
    private String rejectToNode;

    /**
     * Flowable任务ID
     */
    private String flowableTaskId;
}
