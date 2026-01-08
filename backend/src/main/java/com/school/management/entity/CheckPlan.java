package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 检查计划实体
 *
 * @author system
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_plans")
public class CheckPlan extends BaseEntity {

    /**
     * 计划编号(自动生成,如PLAN-20251204-0001)
     */
    private String planCode;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 计划描述
     */
    private String description;

    /**
     * 模板ID(创建时关联的模板)
     */
    private Long templateId;

    /**
     * 模板名称(冗余存储)
     */
    private String templateName;

    /**
     * 模板快照(包含完整的类别+扣分项配置)
     */
    private String templateSnapshot;

    /**
     * 计划开始日期
     */
    private LocalDate startDate;

    /**
     * 计划结束日期
     */
    private LocalDate endDate;

    /**
     * 加权方案ID
     */
    private Long weightConfigId;

    /**
     * 是否启用加权(0=否,1=是)
     */
    private Integer enableWeight;

    /**
     * 自定义标准人数
     */
    private Integer customStandardSize;

    /**
     * 分类/扣分项级别的加权配置列表(多配置模式,JSON格式存储)
     */
    private String itemWeightConfigs;

    /**
     * 状态(0=草稿,1=进行中,2=已结束,3=已归档)
     */
    private Integer status;

    /**
     * 检查次数
     */
    private Integer totalChecks;

    /**
     * 检查记录数
     */
    private Integer totalRecords;

    /**
     * 总扣分
     */
    private BigDecimal totalDeductionScore;

    /**
     * 评优方案ID(预留)
     */
    private Long ratingConfigId;

    /**
     * 统计配置(预留)
     */
    private String statisticsConfig;

    /**
     * 目标范围类型: all=全部, department=按院系, grade=按年级, custom=自定义
     */
    private String targetScopeType;

    /**
     * 目标范围配置(JSON格式)
     * 格式: {departmentIds:[], gradeIds:[], classIds:[], excludeClassIds:[]}
     */
    private String targetScopeConfig;

    /**
     * 创建人ID
     * 重写BaseEntity中的字段,在此表中该字段实际存在
     */
    @TableField(value = "created_by", exist = true)
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String creatorName;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 加权方案名称(关联查询)
     */
    @TableField(exist = false)
    private String weightConfigName;

    /**
     * 状态描述
     */
    @TableField(exist = false)
    private String statusText;

    /**
     * 获取状态描述
     */
    public String getStatusText() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "进行中";
            case 2 -> "已结束";
            case 3 -> "已归档";
            default -> "未知";
        };
    }
}
