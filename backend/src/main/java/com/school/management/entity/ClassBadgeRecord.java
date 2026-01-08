package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 班级徽章获得记录实体
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
@TableName("class_badge_record")
public class ClassBadgeRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 徽章ID
     */
    private Long badgeId;

    /**
     * 徽章名称（冗余）
     */
    private String badgeName;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称（冗余）
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称（冗余）
     */
    private String gradeName;

    /**
     * 院系ID
     */
    private Long departmentId;

    /**
     * 院系名称（冗余）
     */
    private String departmentName;

    /**
     * 授予时间
     */
    private LocalDateTime grantedAt;

    /**
     * 授予人ID（手动授予时有值）
     */
    private Long grantedBy;

    /**
     * 统计周期开始
     */
    private LocalDate periodStart;

    /**
     * 统计周期结束
     */
    private LocalDate periodEnd;

    /**
     * 成就数据JSON
     * 格式：{"frequency": 15, "rank": 1, "rate": 75.0, "consecutiveCount": 5}
     */
    private String achievementData;

    /**
     * 荣誉证书URL
     */
    private String certificateUrl;

    /**
     * 是否已生成证书
     */
    private Boolean certificateGenerated;

    /**
     * 是否已撤销
     */
    private Boolean revoked;

    /**
     * 撤销时间
     */
    private LocalDateTime revokedAt;

    /**
     * 撤销人ID
     */
    private Long revokedBy;

    /**
     * 撤销原因
     */
    private String revokedReason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
