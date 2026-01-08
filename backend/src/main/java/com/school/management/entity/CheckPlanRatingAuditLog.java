package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评级审核日志实体
 */
@Data
@TableName("check_plan_rating_audit_log")
public class CheckPlanRatingAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 评级结果ID
     */
    private Long resultId;

    /**
     * 操作类型: APPROVE REJECT MODIFY PUBLISH UNPUBLISH
     */
    private String action;

    /**
     * 操作前状态
     */
    private Integer beforeStatus;

    /**
     * 操作后状态
     */
    private Integer afterStatus;

    /**
     * 修改前等级ID
     */
    private Long beforeLevelId;

    /**
     * 修改后等级ID
     */
    private Long afterLevelId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作备注
     */
    private String remark;

    private LocalDateTime createdAt;

    // 操作类型常量
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_MODIFY = "MODIFY";
    public static final String ACTION_PUBLISH = "PUBLISH";
    public static final String ACTION_UNPUBLISH = "UNPUBLISH";
}
