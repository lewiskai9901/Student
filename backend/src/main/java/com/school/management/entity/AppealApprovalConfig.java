package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申诉审批流配置实体类
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
@TableName("appeal_approval_configs")
public class AppealApprovalConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 申诉配置ID
     */
    private Long appealConfigId;

    /**
     * 审批步骤序号
     */
    private Integer stepOrder;

    /**
     * 步骤名称
     */
    private String stepName;

    /**
     * 审批人类型(GRADE_DIRECTOR=年级主任,ROLE=角色,USER=指定用户,DEPT_LEADER=部门负责人)
     */
    private String approverType;

    /**
     * 角色编码(当type=ROLE时)
     */
    private String approverRoleCode;

    /**
     * 指定用户ID(当type=USER时)
     */
    private Long approverUserId;

    /**
     * 部门编码(当type=DEPT_LEADER时)
     */
    private String approverDeptCode;

    /**
     * 会签模式(AND=会签都同意,OR=或签任意一人)
     */
    private String signMode;

    /**
     * 是否必须(1=必须,0=可选)
     */
    private Integer isRequired;

    /**
     * 触发条件(JSON格式)
     * 示例: {"scoreChange": {">": 5}}
     */
    private String triggerCondition;

    /**
     * 超时小时数
     */
    private Integer timeoutHours;

    /**
     * 超时动作(REMIND=提醒,ESCALATE=升级,AUTO_PASS=自动通过,AUTO_REJECT=自动驳回)
     */
    private String timeoutAction;

    /**
     * 提醒间隔(小时)
     */
    private Integer remindIntervalHours;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 审批人姓名(当type=USER时)
     */
    @TableField(exist = false)
    private String approverName;

    /**
     * 角色名称(当type=ROLE时)
     */
    @TableField(exist = false)
    private String roleName;

    /**
     * 部门名称(当type=DEPT_LEADER时)
     */
    @TableField(exist = false)
    private String deptName;
}
