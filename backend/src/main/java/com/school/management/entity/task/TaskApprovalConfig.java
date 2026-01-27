package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务审批配置实体（按组织单元分别配置审批人）
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
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 组织单元名称
     */
    private String orgUnitName;

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
