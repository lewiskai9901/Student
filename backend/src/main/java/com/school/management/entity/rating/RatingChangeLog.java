package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评级变更日志
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("rating_change_log")
public class RatingChangeLog {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评级结果ID
     */
    private Long ratingResultId;

    /**
     * 变更类型：CREATED/RECALCULATED/APPROVED/PUBLISHED/REVOKED
     */
    private String changeType;

    /**
     * 变更原因
     */
    private String changeReason;

    /**
     * 旧状态
     */
    private String oldStatus;

    /**
     * 新状态
     */
    private String newStatus;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 关联申诉ID（如因申诉重算）
     */
    private Long relatedAppealId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
