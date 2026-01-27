package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务执行人DTO
 */
@Data
public class TaskAssigneeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 执行人ID
     */
    private Long assigneeId;

    /**
     * 执行人姓名
     */
    private String assigneeName;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 组织单元名称
     */
    private String orgUnitName;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 接收时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptedAt;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 当前审批级别
     */
    private Integer currentApprovalLevel;

    /**
     * 执行人的提交记录
     */
    private TaskSubmissionDTO submission;

    /**
     * 执行人的审批记录列表
     */
    private List<TaskApprovalRecordDTO> approvalRecords;
}
