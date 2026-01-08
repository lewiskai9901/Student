package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "tasks", autoResultMap = true)
public class Task extends BaseEntity {

    /**
     * 任务编号(如: TASK-20251227-0001)
     */
    private String taskCode;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 优先级: 1-紧急, 2-普通, 3-低
     */
    private Integer priority;

    /**
     * 分配人ID(创建任务的领导)
     */
    private Long assignerId;

    /**
     * 分配人姓名
     */
    private String assignerName;

    /**
     * 分配类型: 1-指定个人, 2-批量分配
     */
    private Integer assignType;

    /**
     * 部门ID(任务所属部门)
     */
    private Long departmentId;

    /**
     * 截止时间
     */
    private LocalDateTime dueDate;

    /**
     * 任务状态: 0-待接收, 1-进行中, 2-已提交, 3-已完成, 4-已拒绝
     */
    private Integer status;

    /**
     * 目标ID列表(批量分配时的执行人ID列表)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> targetIds;

    /**
     * 执行人ID(单人任务时)
     */
    @TableField(exist = false)
    private Long assigneeId;

    /**
     * 接受时间
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
     * 流程模板ID
     */
    private Long workflowTemplateId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 当前审批节点
     */
    private String currentNode;

    /**
     * 当前审批人列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> currentApprovers;

    /**
     * 任务附件ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> attachmentIds;

    /**
     * 乐观锁版本号
     * 用于防止并发修改冲突，每次更新时自动递增
     */
    @Version
    private Integer version;

    // ========== 非数据库字段 ==========

    /**
     * 执行人列表(批量分配时)
     */
    @TableField(exist = false)
    private List<TaskAssignee> assignees;

    /**
     * 审批记录列表
     */
    @TableField(exist = false)
    private List<TaskApprovalRecord> approvalRecords;
}
