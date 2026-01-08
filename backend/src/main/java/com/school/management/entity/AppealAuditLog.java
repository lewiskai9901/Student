package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申诉审计日志实体类
 * 记录所有申诉的操作历史
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
@TableName("appeal_audit_logs")
public class AppealAuditLog {

    @TableId(type = IdType.ASSIGN_ID)
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
    private LocalDateTime actionTime;

    /**
     * 操作前状态
     */
    private Integer beforeStatus;

    /**
     * 操作后状态
     */
    private Integer afterStatus;

    /**
     * 操作前分数
     */
    private BigDecimal beforeScore;

    /**
     * 操作后分数
     */
    private BigDecimal afterScore;

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
    private LocalDateTime createdAt;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 操作类型描述
     */
    @TableField(exist = false)
    private String actionTypeDesc;

    /**
     * 操作人信息
     */
    @TableField(exist = false)
    private User actionUser;

    /**
     * 分数变化
     */
    @TableField(exist = false)
    private BigDecimal scoreChange;

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
     * 计算分数变化
     */
    public BigDecimal getScoreChange() {
        if (beforeScore == null || afterScore == null) {
            return BigDecimal.ZERO;
        }
        return afterScore.subtract(beforeScore);
    }
}
