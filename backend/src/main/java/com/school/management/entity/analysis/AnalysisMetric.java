package com.school.management.entity.analysis;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计指标配置实体
 */
@Data
@TableName(value = "stat_analysis_metrics", autoResultMap = true)
public class AnalysisMetric {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分析配置ID
     */
    private Long configId;

    /**
     * 指标编码
     */
    private String metricCode;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 指标描述
     */
    private String metricDesc;

    /**
     * 指标类型
     */
    private String metricType;

    /**
     * 数据来源类型: all-全部类别, category-指定类别, item-指定扣分项
     */
    private String sourceType;

    /**
     * 来源类别ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> sourceCategoryIds;

    /**
     * 来源扣分项ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> sourceItemIds;

    /**
     * 聚合方式: sum-求和, avg-平均, max-最大, min-最小, count-计数
     */
    private String aggregation;

    /**
     * 分组维度: class-班级, grade-年级, department-部门, category-类别, date-日期
     */
    private String groupBy;

    /**
     * 自定义计算公式
     */
    private String customFormula;

    /**
     * 显示格式
     */
    private String displayFormat;

    /**
     * 小数位数
     */
    private Integer decimalPlaces;

    /**
     * 单位
     */
    private String unit;

    /**
     * 图表类型: number-数字卡片, bar-柱状图, line-折线图, pie-饼图, table-表格, rank-排行榜
     */
    private String chartType;

    /**
     * 图表配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> chartConfig;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向: asc-升序, desc-降序
     */
    private String sortOrder;

    /**
     * 只显示前N条
     */
    private Integer topN;

    /**
     * 高亮规则JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> highlightRules;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

    /**
     * 网格位置配置
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> gridPosition;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 指标类型常量 ====================

    public static final String TYPE_TOTAL_SCORE = "total_score";
    public static final String TYPE_AVG_SCORE = "avg_score";
    public static final String TYPE_CHECK_COUNT = "check_count";
    public static final String TYPE_COVERAGE_RATE = "coverage_rate";
    public static final String TYPE_ITEM_COUNT = "item_count";
    public static final String TYPE_PERSON_COUNT = "person_count";
    public static final String TYPE_WEIGHTED_SCORE = "weighted_score";
    public static final String TYPE_RANKING = "ranking";
    public static final String TYPE_TREND = "trend";
    public static final String TYPE_DISTRIBUTION = "distribution";
    public static final String TYPE_OVERVIEW = "overview";
    public static final String TYPE_CUSTOM = "custom";

    // ==================== 图表类型常量 ====================

    public static final String CHART_NUMBER = "number";
    public static final String CHART_BAR = "bar";
    public static final String CHART_LINE = "line";
    public static final String CHART_PIE = "pie";
    public static final String CHART_TABLE = "table";
    public static final String CHART_RANK = "rank";

    // ==================== 聚合方式常量 ====================

    public static final String AGG_SUM = "sum";
    public static final String AGG_AVG = "avg";
    public static final String AGG_MAX = "max";
    public static final String AGG_MIN = "min";
    public static final String AGG_COUNT = "count";

    // ==================== 分组维度常量 ====================

    public static final String GROUP_CLASS = "class";
    public static final String GROUP_GRADE = "grade";
    public static final String GROUP_DEPARTMENT = "department";
    public static final String GROUP_CATEGORY = "category";
    public static final String GROUP_DATE = "date";
}
