package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务提交记录DTO
 */
@Data
public class TaskSubmissionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

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
     * 附件ID列表
     */
    private List<Long> attachmentIds;

    /**
     * 附件URL列表
     */
    private List<String> attachmentUrls;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    /**
     * 审核状态: 0-待审核, 1-审核中, 2-通过, 3-打回
     */
    private Integer reviewStatus;

    /**
     * 审核状态文本
     */
    private String reviewStatusText;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finalReviewedAt;

    /**
     * 被打回次数
     */
    private Integer rejectCount;

    /**
     * 审批记录列表
     */
    private List<TaskApprovalRecordDTO> approvalRecords;
}
