package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 荣誉徽章配置实体
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
@TableName("rating_honor_badge")
public class RatingHonorBadge {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 徽章名称
     */
    private String badgeName;

    /**
     * 徽章图标URL
     */
    private String badgeIcon;

    /**
     * 徽章等级：GOLD/SILVER/BRONZE
     */
    private String badgeLevel;

    /**
     * 关联评级规则ID
     */
    private Long ruleId;

    /**
     * 授予条件配置JSON
     * 格式示例：
     * {
     *   "conditionType": "FREQUENCY",
     *   "frequencyThreshold": 15,
     *   "consecutiveThreshold": 5,
     *   "periodType": "MONTH",
     *   "levelId": 1
     * }
     */
    private String grantCondition;

    /**
     * 徽章描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否自动授予
     */
    private Boolean autoGrant;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除
     */
    @TableLogic
    private Boolean deleted;
}
