package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申诉审计日志DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class AppealAuditLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 申诉ID
     */
    private Long appealId;

    /**
     * 操作类型(1=提交,2=审核,3=撤销,4=修改,5=公示,6=生效)
     */
    private Integer actionType;

    /**
     * 操作类型描述
     */
    private String actionTypeDesc;

    /**
     * 操作人ID
     */
    private Long actionUserId;

    /**
     * 操作人姓名
     */
    private String actionUserName;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actionTime;

    /**
     * 操作前状态
     */
    private Integer beforeStatus;

    /**
     * 操作前状态描述
     */
    private String beforeStatusDesc;

    /**
     * 操作后状态
     */
    private Integer afterStatus;

    /**
     * 操作后状态描述
     */
    private String afterStatusDesc;

    /**
     * 操作前分数
     */
    private BigDecimal beforeScore;

    /**
     * 操作后分数
     */
    private BigDecimal afterScore;

    /**
     * 分数变化
     */
    private BigDecimal scoreChange;

    /**
     * 操作说明
     */
    private String comment;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 获取操作类型描述
     */
    public String getActionTypeDesc() {
        if (actionType == null) {
            return "未知";
        }
        switch (actionType) {
            case 1:
                return "提交申诉";
            case 2:
                return "审核申诉";
            case 3:
                return "撤销申诉";
            case 4:
                return "修改申诉";
            case 5:
                return "进入公示";
            case 6:
                return "生效";
            default:
                return "未知操作";
        }
    }

    /**
     * 获取状态描述
     */
    public String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "待审核";
            case 2:
                return "审核通过";
            case 3:
                return "审核驳回";
            case 4:
                return "已撤销";
            case 5:
                return "已过期";
            case 6:
                return "公示中";
            case 7:
                return "已生效";
            default:
                return "未知";
        }
    }

    public String getBeforeStatusDesc() {
        return getStatusDesc(beforeStatus);
    }

    public String getAfterStatusDesc() {
        return getStatusDesc(afterStatus);
    }
}
