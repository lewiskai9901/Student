package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日常检查类别实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@TableName("daily_check_categories")
public class DailyCheckCategory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查ID
     */
    private Long checkId;

    /**
     * 检查类别ID
     */
    private Long categoryId;

    /**
     * 类别名称(冗余)
     */
    private String categoryName;

    /**
     * 关联类型 0不关联 1关联宿舍 2关联教室
     */
    private Integer linkType;

    /**
     * 是否必检
     */
    private Integer isRequired;

    /**
     * 本次检查轮次数（已废弃，保留兼容）
     */
    private Integer checkRounds;

    /**
     * 参与的轮次，逗号分隔，如"1,3"表示参与第1轮和第3轮
     */
    private String participatedRounds;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
