package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查导出模板实体
 */
@Data
@TableName("check_export_templates")
public class CheckExportTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联的检查计划ID
     */
    private Long planId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 筛选配置（JSON格式）
     * 包含：deductionItemIds, checkRounds, categoryIds等
     */
    private String filterConfig;

    /**
     * 表格字段配置（JSON格式）
     * 包含：columns, showDepartmentHeader, showGradeHeader, showClassHeader等
     */
    private String tableConfig;

    /**
     * 分组方式：department,grade,class
     */
    private String groupBy;

    /**
     * 排序字段
     */
    private String sortBy;

    /**
     * 输出格式：PDF/WORD/EXCEL
     */
    private String outputFormat;

    /**
     * 富文本文档模板
     */
    private String documentTemplate;

    /**
     * 自定义变量配置（JSON格式）
     * 格式: [{"name": "schoolName", "label": "学校名称", "defaultValue": "XX学院"}, ...]
     */
    private String customVariables;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态：0禁用 1启用
     */
    private Integer status;

    /**
     * 创建人
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
     * 删除标记
     */
    @TableLogic
    private Integer deleted;
}
