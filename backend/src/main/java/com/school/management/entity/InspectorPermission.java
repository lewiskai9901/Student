package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打分人员权限配置实体
 */
@Data
@TableName("inspector_permissions")
public class InspectorPermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 打分人员ID（关联check_plan_inspectors）
     */
    private Long inspectorId;

    /**
     * 检查计划ID（冗余，便于查询）
     */
    private Long planId;

    /**
     * 用户ID（冗余，便于查询）
     */
    private Long userId;

    /**
     * 检查类别ID
     */
    private String categoryId;

    /**
     * 检查类别名称（冗余）
     */
    private String categoryName;

    /**
     * 可检查的班级ID列表JSON，NULL表示计划范围内全部
     */
    private String classIds;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
