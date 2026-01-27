package com.school.management.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分析结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分析结果")
public class AnalysisResultDTO {

    @Schema(description = "配置ID")
    private Long configId;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "数据起始日期")
    private LocalDate dateRangeStart;

    @Schema(description = "数据截止日期")
    private LocalDate dateRangeEnd;

    @Schema(description = "生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "是否动态更新")
    private Boolean isDynamic;

    // ==================== 统计概览 ====================

    @Schema(description = "统计概览")
    private OverviewDTO overview;

    // ==================== 各指标结果 ====================

    @Schema(description = "指标结果列表")
    private List<MetricResultDTO> metricResults;

    // ==================== 内部类 ====================

    /**
     * 概览数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "概览数据")
    public static class OverviewDTO {
        @Schema(description = "检查记录数")
        private Integer recordCount;

        @Schema(description = "涉及班级数")
        private Integer classCount;

        @Schema(description = "总扣分")
        private BigDecimal totalScore;

        @Schema(description = "平均扣分")
        private BigDecimal avgScore;

        @Schema(description = "最高扣分")
        private BigDecimal maxScore;

        @Schema(description = "最低扣分")
        private BigDecimal minScore;

        @Schema(description = "扣分项总数")
        private Integer totalItems;

        @Schema(description = "涉及人次")
        private Integer totalPersons;

        @Schema(description = "平均覆盖率")
        private BigDecimal avgCoverageRate;

        @Schema(description = "全覆盖班级数")
        private Integer fullCoverageCount;
    }

    /**
     * 指标结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "指标结果")
    public static class MetricResultDTO {
        @Schema(description = "指标ID")
        private Long metricId;

        @Schema(description = "指标编码")
        private String metricCode;

        @Schema(description = "指标名称")
        private String metricName;

        @Schema(description = "指标类型")
        private String metricType;

        @Schema(description = "图表类型")
        private String chartType;

        @Schema(description = "数据")
        private Object data;

        @Schema(description = "图表配置")
        private Map<String, Object> chartConfig;

        @Schema(description = "网格位置")
        private Map<String, Object> gridPosition;
    }

    /**
     * 班级排名项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "班级排名项")
    public static class ClassRankingItemDTO {
        @Schema(description = "班级ID")
        private Long classId;

        @Schema(description = "班级名称")
        private String className;

        @Schema(description = "年级ID")
        private Long gradeId;

        @Schema(description = "年级名称")
        private String gradeName;

        @Schema(description = "组织单元ID（原部门ID）")
        private Long orgUnitId;

        @Schema(description = "组织单元名称（原部门名称）")
        private String orgUnitName;

        @Schema(description = "班主任姓名")
        private String teacherName;

        @Schema(description = "检查次数")
        private Integer checkCount;

        @Schema(description = "应检查次数")
        private Integer expectedCheckCount;

        @Schema(description = "覆盖率")
        private BigDecimal coverageRate;

        @Schema(description = "总扣分")
        private BigDecimal totalScore;

        @Schema(description = "平均扣分")
        private BigDecimal avgScore;

        @Schema(description = "加权扣分")
        private BigDecimal weightedScore;

        @Schema(description = "排名")
        private Integer ranking;

        @Schema(description = "年级排名")
        private Integer gradeRanking;

        @Schema(description = "评级等级")
        private String scoreLevel;

        @Schema(description = "与均值差距")
        private BigDecimal vsAvg;

        @Schema(description = "类别扣分明细")
        private List<CategoryScoreDTO> categoryScores;
    }

    /**
     * 类别扣分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "类别扣分")
    public static class CategoryScoreDTO {
        @Schema(description = "类别ID")
        private Long categoryId;

        @Schema(description = "类别名称")
        private String categoryName;

        @Schema(description = "扣分")
        private BigDecimal score;

        @Schema(description = "占比")
        private BigDecimal percentage;
    }

    /**
     * 趋势数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "趋势数据点")
    public static class TrendPointDTO {
        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "日期标签")
        private String dateLabel;

        @Schema(description = "检查次数")
        private Integer checkCount;

        @Schema(description = "总扣分")
        private BigDecimal totalScore;

        @Schema(description = "平均扣分")
        private BigDecimal avgScore;

        @Schema(description = "班级数")
        private Integer classCount;

        @Schema(description = "人次")
        private Integer personCount;
    }

    /**
     * 分布数据项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分布数据项")
    public static class DistributionItemDTO {
        @Schema(description = "名称")
        private String name;

        @Schema(description = "值")
        private BigDecimal value;

        @Schema(description = "占比")
        private BigDecimal percentage;

        @Schema(description = "数量")
        private Integer count;
    }
}
