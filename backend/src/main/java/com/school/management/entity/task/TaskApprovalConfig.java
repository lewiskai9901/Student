package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务审批配置实体（按系部分别配置审批人）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_approval_configs")
public class TaskApprovalConfig extends BaseEntity {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 系部ID
     */
    private Long departmentId;

    /**
     * 系部名称
     */
    private String departmentName;

    /**
     * 审批级别: 1-第一级, 2-第二级, 3-第三级...
     */
    private Integer approvalLevel;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人角色（如：系领导、学工处领导）
     */
    private String approverRole;
}
