package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批流配置DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class ApprovalFlowConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
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
     * 审批人类型描述
     */
    private String approverTypeDesc;

    /**
     * 角色编码(当type=ROLE时)
     */
    private String approverRoleCode;

    /**
     * 角色名称
     */
    private String approverRoleName;

    /**
     * 指定用户ID(当type=USER时)
     */
    private Long approverUserId;

    /**
     * 指定用户姓名
     */
    private String approverUserName;

    /**
     * 部门编码(当type=DEPT_LEADER时)
     */
    private String approverDeptCode;

    /**
     * 部门名称
     */
    private String approverDeptName;

    /**
     * 会签模式(AND=会签都同意,OR=或签任意一人)
     */
    private String signMode;

    /**
     * 会签模式描述
     */
    private String signModeDesc;

    /**
     * 是否必须(1=必须,0=可选)
     */
    private Boolean isRequired;

    /**
     * 触发条件(JSON格式)
     */
    private String triggerCondition;

    /**
     * 触发条件描述
     */
    private String triggerConditionDesc;

    /**
     * 超时小时数
     */
    private Integer timeoutHours;

    /**
     * 超时动作(REMIND=提醒,ESCALATE=升级,AUTO_PASS=自动通过,AUTO_REJECT=自动驳回)
     */
    private String timeoutAction;

    /**
     * 超时动作描述
     */
    private String timeoutActionDesc;

    /**
     * 提醒间隔(小时)
     */
    private Integer remindIntervalHours;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 获取审批人类型描述
     */
    public String getApproverTypeDesc() {
        if (approverType == null) {
            return "未知";
        }
        switch (approverType) {
            case "GRADE_DIRECTOR":
                return "年级主任";
            case "ROLE":
                return "角色: " + (approverRoleName != null ? approverRoleName : approverRoleCode);
            case "USER":
                return "指定用户: " + (approverUserName != null ? approverUserName : "");
            case "DEPT_LEADER":
                return "部门负责人: " + (approverDeptName != null ? approverDeptName : approverDeptCode);
            default:
                return "未知";
        }
    }

    /**
     * 获取会签模式描述
     */
    public String getSignModeDesc() {
        if (signMode == null) {
            return "未知";
        }
        return "AND".equals(signMode) ? "会签(都同意)" : "或签(任意一人)";
    }

    /**
     * 获取超时动作描述
     */
    public String getTimeoutActionDesc() {
        if (timeoutAction == null) {
            return "未知";
        }
        switch (timeoutAction) {
            case "REMIND":
                return "提醒";
            case "ESCALATE":
                return "升级";
            case "AUTO_PASS":
                return "自动通过";
            case "AUTO_REJECT":
                return "自动驳回";
            default:
                return "未知";
        }
    }

    /**
     * 获取触发条件描述
     */
    public String getTriggerConditionDesc() {
        if (triggerCondition == null || triggerCondition.trim().isEmpty() || "{}".equals(triggerCondition)) {
            return "无条件(总是执行)";
        }
        // 这里可以解析JSON并生成人类可读的描述
        // 例如: {"scoreChange": {">": 5}} -> "分数变化大于5分时"
        return triggerCondition;
    }
}
