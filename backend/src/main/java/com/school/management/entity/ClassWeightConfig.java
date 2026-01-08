package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 班级人数加权配置实体类 - 方案库模式
 *
 * 用于配置日常检查的班级人数加权规则,解决不同规模班级的公平评分问题。
 * 核心原则: 班级人数越多,管理难度越大,扣分应相应减少。
 *
 * 【设计模式变更说明】V5.0 版本改为方案库模式:
 * - 旧模式: 全局配置 + 自动应用到所有检查
 * - 新模式: 方案库存储 + 创建检查时手动选择方案
 * - 优势: 灵活性更高,不同检查可使用不同加权策略
 *
 * 加权模式说明:
 * - STANDARD: 标准人数加权 (权重=标准人数/实际人数) 【推荐】
 *   示例: 标准40人,实际50人 → 权重=40/50=0.8 → 扣10分变成8分
 * - PER_CAPITA: 人均加权 (权重=实际人数/标准人数)
 * - SEGMENT: 分段加权 (根据人数区间使用不同权重)
 * - NONE: 不加权 (使用原始扣分)
 *
 * 标准人数配置(已集成到本表):
 * - FIXED: 使用手动配置的固定标准人数
 * - DYNAMIC: 使用实时计算的平均班级人数(同年级同部门)
 * - CUSTOM: 使用自定义规则(JSON格式,按班级/年级/部门设置)
 *
 * 使用流程:
 * 1. 管理员创建多个加权方案(方案库)
 * 2. 创建日常检查时,选择使用哪个加权方案(或不使用)
 * 3. 检查转换为记录时,保存方案快照,确保历史数据准确性
 *
 * @author system
 * @version 5.0.0
 * @since 2025-01-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("class_weight_configs")
public class ClassWeightConfig extends BaseEntity {

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置编码
     */
    private String configCode;

    /**
     * 加权模式(STANDARD=标准折算,PER_CAPITA=人均,SEGMENT=分段,NONE=不加权)
     */
    private String weightMode;

    /**
     * 是否启用加权(0=禁用,1=启用)
     */
    private Integer enableWeight;

    /**
     * [已废弃] 是否使用固定标准
     * 注意: 标准人数来源已统一由 class_size_standards 表的 standardMode 字段控制
     * 此字段保留仅为兼容旧数据,新版本不再使用
     */
    @Deprecated
    private Integer useFixedStandard;

    /**
     * 最小权重系数
     */
    private BigDecimal minWeight;

    /**
     * 最大权重系数
     */
    private BigDecimal maxWeight;

    /**
     * 是否启用权重上下限(0=不限制,1=限制)
     */
    private Integer enableWeightLimit;

    /**
     * 分段规则(JSON格式)
     * 示例: [{"minSize":1,"maxSize":20,"weight":1.5},{"minSize":21,"maxSize":40,"weight":1.0}]
     */
    private String segmentRules;

    /**
     * 应用到所有检查(1=是,0=否)
     * @deprecated 已废弃,改用方案库模式,不再自动应用到所有检查
     */
    @Deprecated
    private Integer applyToAll;

    /**
     * 生效日期
     * @deprecated 已废弃,改用方案库模式,不再使用时间范围控制
     */
    @Deprecated
    private LocalDate effectiveDate;

    /**
     * 失效日期
     * @deprecated 已废弃,改用方案库模式,不再使用时间范围控制
     */
    @Deprecated
    private LocalDate expireDate;

    /**
     * 是否默认配置
     */
    private Integer isDefault;

    /**
     * 状态(0=禁用,1=启用)
     */
    private Integer status;

    /**
     * 配置说明
     */
    private String description;

    /**
     * 学期ID
     * @deprecated 已废弃,改用方案库模式,不再关联学期
     */
    @Deprecated
    private Long semesterId;

    /**
     * 应用范围(GLOBAL=全局,DEPARTMENT=部门,GRADE=年级,CLASS=班级)
     * @deprecated 已废弃,改用方案库模式,不再使用全局应用范围
     */
    @Deprecated
    private String applyScope;

    /**
     * 可见范围(ALL=全部可见, DEPARTMENT=仅部门可见, ROLE=仅角色可见)
     */
    private String visibleScope;

    /**
     * 可见部门ID(当visibleScope=DEPARTMENT时使用)
     */
    private Long visibleDepartmentId;

    /**
     * 适用的检查类型(JSON数组格式,如:["DORM_HYGIENE","CLASS_HYGIENE"])
     */
    private String applicableCheckTypes;

    /**
     * 创建人ID
     * 注意: 数据库实际字段为created_by,此字段仅用于业务逻辑,不映射到数据库
     */
    @TableField(exist = false)
    private Long creatorId;

    /**
     * 创建人姓名
     * 注意: 数据库中不存在此字段,仅用于前端展示
     */
    @TableField(exist = false)
    private String creatorName;

    /**
     * 使用次数统计
     */
    private Integer useCount;

    /**
     * 标准人数模式
     * - FIXED: 固定标准人数
     * - TARGET_AVERAGE: 目标平均人数(根据检查目标自动计算)
     * - RANGE_AVERAGE: 范围平均人数(根据预设范围计算)
     * - CUSTOM: 自定义规则(已废弃)
     */
    private String standardSizeMode;

    /**
     * 固定标准人数(当standardSizeMode=FIXED时使用)
     */
    private Integer standardSize;

    /**
     * 自定义标准人数规则(JSON格式,按班级/年级/部门设置)
     * 示例: {"classes":{"1":45},"grades":{"2023":40},"departments":{"101":42}}
     */
    private String customStandardRules;

    /**
     * 选择的部门范围(JSON数组,当standardSizeMode=RANGE_AVERAGE时使用)
     * 示例: [1,2,3]
     */
    private String rangeDepartments;

    /**
     * 选择的年级范围(JSON数组,当standardSizeMode=RANGE_AVERAGE时使用)
     * 示例: ["2024","2023"]
     */
    private String rangeGrades;

    /**
     * 选择的班级范围(JSON数组,当standardSizeMode=RANGE_AVERAGE时使用)
     * 示例: [101,102,103]
     */
    private String rangeClasses;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 类别加权规则列表
     */
    @TableField(exist = false)
    private java.util.List<CategoryWeightRule> categoryRules;

    /**
     * 是否已过期
     */
    @TableField(exist = false)
    private Boolean expired;

    /**
     * 加权模式描述
     */
    @TableField(exist = false)
    private String weightModeDesc;

    /**
     * 获取加权模式描述
     */
    public String getWeightModeDesc() {
        if (weightMode == null) {
            return "未知";
        }
        switch (weightMode) {
            case "STANDARD":
                return "标准人数折算";
            case "PER_CAPITA":
                return "人均扣分";
            case "SEGMENT":
                return "分段加权";
            case "NONE":
                return "不加权";
            default:
                return "未知";
        }
    }

    /**
     * 判断是否已过期
     */
    public Boolean getExpired() {
        if (expireDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expireDate);
    }
}
