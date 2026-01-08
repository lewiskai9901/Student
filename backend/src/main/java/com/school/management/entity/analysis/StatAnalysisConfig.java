package com.school.management.entity.analysis;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统计分析配置实体
 */
@Data
@TableName(value = "stat_analysis_configs", autoResultMap = true)
public class StatAnalysisConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属检查计划ID
     */
    private Long planId;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置描述
     */
    private String configDesc;

    /**
     * 范围类型: time-时间范围, record-记录选择, mixed-混合
     */
    private String scopeType;

    /**
     * 范围配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> scopeConfig;

    /**
     * 目标类型: all-全部, department-部门, grade-年级, custom-自定义
     */
    private String targetType;

    /**
     * 目标配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> targetConfig;

    /**
     * 更新模式: static-静态, dynamic-动态
     */
    private String updateMode;

    /**
     * 是否自动刷新
     */
    private Boolean autoRefresh;

    /**
     * 刷新间隔(分钟)
     */
    private Integer refreshInterval;

    /**
     * 上次刷新时间
     */
    private LocalDateTime lastRefreshTime;

    /**
     * 缺检策略: avg-平均扣分, weighted-加权平均, full_only-仅全覆盖, penalty-缺检惩罚, exempt-缺检豁免
     */
    private String missingStrategy;

    /**
     * 缺检策略配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> missingStrategyConfig;

    /**
     * 布局配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> layoutConfig;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 是否默认配置
     */
    private Boolean isDefault;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者姓名
     */
    private String creatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    // ==================== 常量定义 ====================

    public static final String SCOPE_TYPE_TIME = "time";
    public static final String SCOPE_TYPE_RECORD = "record";
    public static final String SCOPE_TYPE_MIXED = "mixed";

    public static final String TARGET_TYPE_ALL = "all";
    public static final String TARGET_TYPE_DEPARTMENT = "department";
    public static final String TARGET_TYPE_GRADE = "grade";
    public static final String TARGET_TYPE_CUSTOM = "custom";

    public static final String UPDATE_MODE_STATIC = "static";
    public static final String UPDATE_MODE_DYNAMIC = "dynamic";

    public static final String MISSING_STRATEGY_AVG = "avg";
    public static final String MISSING_STRATEGY_WEIGHTED = "weighted";
    public static final String MISSING_STRATEGY_FULL_ONLY = "full_only";
    public static final String MISSING_STRATEGY_PENALTY = "penalty";
    public static final String MISSING_STRATEGY_EXEMPT = "exempt";
}
