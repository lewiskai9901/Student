package com.school.management.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计指标配置DTO
 */
@Data
@Schema(description = "统计指标配置")
public class AnalysisMetricDTO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "分析配置ID")
    private Long configId;

    @Schema(description = "指标编码", required = true)
    @NotBlank(message = "指标编码不能为空")
    @Size(max = 50, message = "指标编码不能超过50字符")
    private String metricCode;

    @Schema(description = "指标名称", required = true)
    @NotBlank(message = "指标名称不能为空")
    @Size(max = 100, message = "指标名称不能超过100字符")
    private String metricName;

    @Schema(description = "指标描述")
    @Size(max = 500, message = "指标描述不能超过500字符")
    private String metricDesc;

    // ==================== 指标类型 ====================

    @Schema(description = "指标类型: total_score, avg_score, check_count, coverage_rate, item_count, person_count, weighted_score, ranking, trend, distribution, overview, custom", required = true)
    @NotBlank(message = "指标类型不能为空")
    private String metricType;

    // ==================== 数据来源 ====================

    @Schema(description = "数据来源类型: all-全部类别, category-指定类别, item-指定扣分项")
    private String sourceType = "all";

    @Schema(description = "来源类别ID列表")
    private List<Long> sourceCategoryIds;

    @Schema(description = "来源扣分项ID列表")
    private List<Long> sourceItemIds;

    // ==================== 聚合配置 ====================

    @Schema(description = "聚合方式: sum-求和, avg-平均, max-最大, min-最小, count-计数")
    private String aggregation = "sum";

    @Schema(description = "分组维度: class-班级, grade-年级, department-部门, category-类别, date-日期")
    private String groupBy;

    // ==================== 自定义公式 ====================

    @Schema(description = "自定义计算公式")
    @Size(max = 500, message = "自定义公式不能超过500字符")
    private String customFormula;

    // ==================== 显示配置 ====================

    @Schema(description = "显示格式")
    private String displayFormat = "{value}";

    @Schema(description = "小数位数")
    private Integer decimalPlaces = 2;

    @Schema(description = "单位")
    private String unit;

    // ==================== 图表配置 ====================

    @Schema(description = "图表类型: number-数字卡片, bar-柱状图, line-折线图, pie-饼图, table-表格, rank-排行榜")
    private String chartType = "number";

    @Schema(description = "图表配置")
    private Map<String, Object> chartConfig;

    // ==================== 排序配置 ====================

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "排序方向: asc-升序, desc-降序")
    private String sortOrder = "asc";

    @Schema(description = "只显示前N条")
    private Integer topN;

    // ==================== 高亮配置 ====================

    @Schema(description = "高亮规则")
    private List<HighlightRuleDTO> highlightRules;

    // ==================== 状态 ====================

    @Schema(description = "是否启用")
    private Boolean isEnabled = true;

    @Schema(description = "显示顺序")
    private Integer displayOrder = 0;

    @Schema(description = "网格位置配置")
    private GridPositionDTO gridPosition;

    // ==================== 元数据 ====================

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // ==================== 内部类 ====================

    /**
     * 高亮规则
     */
    @Data
    @Schema(description = "高亮规则")
    public static class HighlightRuleDTO {
        @Schema(description = "条件类型: gt-大于, gte-大于等于, lt-小于, lte-小于等于, eq-等于, between-区间")
        private String condition;

        @Schema(description = "阈值")
        private Double threshold;

        @Schema(description = "区间最小值")
        private Double minValue;

        @Schema(description = "区间最大值")
        private Double maxValue;

        @Schema(description = "颜色")
        private String color;

        @Schema(description = "背景色")
        private String backgroundColor;

        @Schema(description = "标签")
        private String label;
    }

    /**
     * 网格位置配置
     */
    @Data
    @Schema(description = "网格位置配置")
    public static class GridPositionDTO {
        @Schema(description = "X坐标")
        private Integer x;

        @Schema(description = "Y坐标")
        private Integer y;

        @Schema(description = "宽度(格数)")
        private Integer w;

        @Schema(description = "高度(格数)")
        private Integer h;
    }
}
