package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务提交记录实体(班主任提交的完成情况)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "task_submissions", autoResultMap = true)
public class TaskSubmission extends BaseEntity {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务执行人记录ID
     */
    private Long taskAssigneeId;

    /**
     * 提交人ID
     */
    private Long submitterId;

    /**
     * 提交人姓名
     */
    private String submitterName;

    /**
     * 完成情况说明
     */
    private String content;

    /**
     * 附件ID列表(文件/照片)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> attachmentIds;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 审核状态: 0-待审核, 1-审核中, 2-通过, 3-打回
     */
    private Integer reviewStatus;

    /**
     * 最终审核人ID
     */
    private Long finalReviewerId;

    /**
     * 最终审核人姓名
     */
    private String finalReviewerName;

    /**
     * 最终审核意见
     */
    private String finalReviewComment;

    /**
     * 最终审核时间
     */
    private LocalDateTime finalReviewedAt;

    /**
     * 被打回次数
     */
    private Integer rejectCount;

    /**
     * 打回到的节点
     */
    private String rejectToNode;

    // ========== 非数据库字段 ==========

    /**
     * 审批记录列表
     */
    @TableField(exist = false)
    private List<TaskApprovalRecord> approvalRecords;
}
