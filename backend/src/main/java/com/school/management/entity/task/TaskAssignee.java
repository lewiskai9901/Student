package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务执行人实体(支持一个任务多个执行人)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "task_assignees", autoResultMap = true)
public class TaskAssignee extends BaseEntity {

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
     * 审批配置 (审批流程规则、审批人等)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> approvalConfig;

    /**
     * 状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回
     */
    private Integer status;

    /**
     * 接收时间
     */
    private LocalDateTime acceptedAt;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 该执行人的流程实例ID
     */
    private String processInstanceId;

    /**
     * 当前审批级别 (0-未审批, 1-一级审批, 2-二级审批, etc.)
     */
    private Integer currentApprovalLevel;
}
