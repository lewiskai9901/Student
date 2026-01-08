package com.school.management.entity.analysis;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 类别映射实体 - 用于统一不同模板的检查类别
 */
@Data
@TableName(value = "stat_category_mappings", autoResultMap = true)
public class CategoryMapping {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分析配置ID (用于新版统计分析)
     */
    private Long configId;

    /**
     * 检查计划ID (用于旧版统一类别管理)
     */
    private Long planId;

    /**
     * 模板类别ID (源类别)
     */
    private Long templateCategoryId;

    /**
     * 模板类别名称
     */
    private String templateCategoryName;

    /**
     * 统一类别ID (映射目标)
     */
    private Long unifiedCategoryId;

    /**
     * 统一类别名称 (映射目标)
     */
    private String unifiedCategoryName;

    /**
     * 统一类别编码
     */
    private String unifiedCode;

    /**
     * 统一类别名称
     */
    private String unifiedName;

    /**
     * 统一类别类型: HYGIENE-卫生, DISCIPLINE-纪律, ATTENDANCE-考勤, DORMITORY-宿舍, OTHER-其他
     */
    private String unifiedType;

    /**
     * 源类别ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> sourceCategoryIds;

    /**
     * 源类别名称列表(冗余)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sourceCategoryNames;

    /**
     * 显示颜色
     */
    private String color;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 类别类型常量 ====================

    public static final String TYPE_HYGIENE = "HYGIENE";
    public static final String TYPE_DISCIPLINE = "DISCIPLINE";
    public static final String TYPE_ATTENDANCE = "ATTENDANCE";
    public static final String TYPE_DORMITORY = "DORMITORY";
    public static final String TYPE_OTHER = "OTHER";
}
