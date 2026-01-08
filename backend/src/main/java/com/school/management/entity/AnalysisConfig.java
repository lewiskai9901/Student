package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计分析配置实体类
 * 对应表: analysis_configs
 *
 * @author Claude
 * @since 2025-12-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "analysis_configs", autoResultMap = true)
public class AnalysisConfig extends BaseEntity {

    /**
     * 配置名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 关联的检查计划ID（当targetType为PLAN时使用）
     */
    private Long planId;

    /**
     * 分析目标类型: TEMPLATE/CATEGORY/DEDUCTION_ITEM/SINGLE_CHECK/ORGANIZATION/PLAN
     */
    private String targetType;

    /**
     * 检查模板ID（当targetType为TEMPLATE时使用）
     */
    private Long templateId;

    /**
     * 检查类别ID（当targetType为CATEGORY时使用）
     */
    private Long categoryId;

    /**
     * 扣分项ID（当targetType为DEDUCTION_ITEM时使用）
     */
    private Long deductionItemId;

    /**
     * 检查记录ID（当targetType为SINGLE_CHECK时使用）
     */
    private Long checkRecordId;

    /**
     * 院系ID（当targetType为ORGANIZATION时使用）
     */
    private Long departmentId;

    /**
     * 班级ID（当targetType为ORGANIZATION时使用）
     */
    private Long classId;

    /**
     * 组织范围类型: ALL/DEPARTMENT/CLASS
     */
    private String orgScopeType;

    /**
     * 指定的院系/班级ID列表（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> orgScopeIds;

    /**
     * 时间范围类型: FIXED/DYNAMIC
     */
    private String timeRangeType;

    /**
     * 固定开始日期
     */
    private LocalDate fixedStartDate;

    /**
     * 固定结束日期
     */
    private LocalDate fixedEndDate;

    /**
     * 动态范围: LAST_7_DAYS/LAST_30_DAYS/THIS_MONTH/THIS_SEMESTER/THIS_YEAR
     */
    private String dynamicRange;

    /**
     * 启用的报告模块（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> reportSections;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 创建人ID（覆盖BaseEntity中的字段，因为数据库中存在）
     */
    @TableField("created_by")
    private Long createdBy;
}
