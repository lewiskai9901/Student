package com.school.management.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计分析报告DTO
 *
 * @author Claude
 * @since 2025-12-05
 */
@Data
@Schema(description = "统计分析报告")
public class AnalysisReportDTO {

    @Schema(description = "配置ID")
    private Long configId;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "分析目标类型")
    private String targetType;

    @Schema(description = "目标名称")
    private String targetName;

    @Schema(description = "时间范围开始")
    private LocalDate startDate;

    @Schema(description = "时间范围结束")
    private LocalDate endDate;

    @Schema(description = "生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "报告数据")
    private ReportData data;

    /**
     * 报告数据
     */
    @Data
    @Schema(description = "报告数据")
    public static class ReportData {

        @Schema(description = "频次统计")
        private FrequencyStats frequencyStats;

        @Schema(description = "分数统计")
        private ScoreStats scoreStats;

        @Schema(description = "趋势分析")
        private TrendAnalysis trendAnalysis;

        @Schema(description = "问题分布")
        private List<DistributionItem> distribution;

        @Schema(description = "班级排名")
        private List<RankingItem> classRanking;

        @Schema(description = "学生排名")
        private List<StudentRankingItem> studentRanking;

        @Schema(description = "扣分热力图数据")
        private HeatmapData deductionHeatmap;

        @Schema(description = "类别雷达图数据")
        private RadarData categoryRadar;

        @Schema(description = "对比分析数据")
        private CompareAnalysis compareAnalysis;
    }

    /**
     * 频次统计
     */
    @Data
    @Schema(description = "频次统计")
    public static class FrequencyStats {

        @Schema(description = "总检查次数")
        private Integer totalChecks;

        @Schema(description = "总扣分次数")
        private Integer totalDeductions;

        @Schema(description = "涉及班级数")
        private Integer classCount;

        @Schema(description = "涉及学生数")
        private Integer studentCount;

        @Schema(description = "各扣分项频次")
        private List<FrequencyItem> itemFrequencies;
    }

    /**
     * 频次项
     */
    @Data
    @Schema(description = "频次项")
    public static class FrequencyItem {

        @Schema(description = "项目名称")
        private String name;

        @Schema(description = "次数")
        private Integer count;

        @Schema(description = "占比")
        private BigDecimal percentage;
    }

    /**
     * 分数统计
     */
    @Data
    @Schema(description = "分数统计")
    public static class ScoreStats {

        @Schema(description = "总扣分")
        private BigDecimal totalDeduction;

        @Schema(description = "平均扣分")
        private BigDecimal avgDeduction;

        @Schema(description = "最高单次扣分")
        private BigDecimal maxDeduction;

        @Schema(description = "最低单次扣分")
        private BigDecimal minDeduction;

        @Schema(description = "平均得分")
        private BigDecimal avgScore;

        @Schema(description = "最高得分")
        private BigDecimal maxScore;

        @Schema(description = "最低得分")
        private BigDecimal minScore;
    }

    /**
     * 趋势分析
     */
    @Data
    @Schema(description = "趋势分析")
    public static class TrendAnalysis {

        @Schema(description = "日期列表")
        private List<String> dates;

        @Schema(description = "扣分趋势")
        private List<BigDecimal> deductionTrend;

        @Schema(description = "得分趋势")
        private List<BigDecimal> scoreTrend;

        @Schema(description = "检查次数趋势")
        private List<Integer> checkCountTrend;
    }

    /**
     * 分布项
     */
    @Data
    @Schema(description = "分布项")
    public static class DistributionItem {

        @Schema(description = "名称")
        private String name;

        @Schema(description = "值")
        private BigDecimal value;

        @Schema(description = "占比")
        private BigDecimal percentage;
    }

    /**
     * 排名项
     */
    @Data
    @Schema(description = "排名项")
    public static class RankingItem {

        @Schema(description = "排名")
        private Integer rank;

        @Schema(description = "ID")
        private Long id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "扣分次数")
        private Integer deductionCount;

        @Schema(description = "总扣分")
        private BigDecimal totalDeduction;

        @Schema(description = "平均得分")
        private BigDecimal avgScore;
    }

    /**
     * 学生排名项
     */
    @Data
    @Schema(description = "学生排名项")
    public static class StudentRankingItem {

        @Schema(description = "排名")
        private Integer rank;

        @Schema(description = "学生ID")
        private Long studentId;

        @Schema(description = "学号")
        private String studentNo;

        @Schema(description = "姓名")
        private String studentName;

        @Schema(description = "班级名称")
        private String className;

        @Schema(description = "扣分次数")
        private Integer deductionCount;

        @Schema(description = "总扣分")
        private BigDecimal totalDeduction;
    }

    /**
     * 热力图数据
     */
    @Data
    @Schema(description = "热力图数据")
    public static class HeatmapData {

        @Schema(description = "X轴标签（日期/时间段）")
        private List<String> xLabels;

        @Schema(description = "Y轴标签（扣分项/类别）")
        private List<String> yLabels;

        @Schema(description = "热力图数据 [x, y, value]")
        private List<List<Object>> data;

        @Schema(description = "最大值")
        private BigDecimal maxValue;
    }

    /**
     * 雷达图数据
     */
    @Data
    @Schema(description = "雷达图数据")
    public static class RadarData {

        @Schema(description = "维度指标")
        private List<RadarIndicator> indicators;

        @Schema(description = "数据系列")
        private List<RadarSeries> series;
    }

    /**
     * 雷达图指标
     */
    @Data
    @Schema(description = "雷达图指标")
    public static class RadarIndicator {

        @Schema(description = "指标名称")
        private String name;

        @Schema(description = "最大值")
        private BigDecimal max;
    }

    /**
     * 雷达图系列
     */
    @Data
    @Schema(description = "雷达图系列")
    public static class RadarSeries {

        @Schema(description = "系列名称")
        private String name;

        @Schema(description = "数据值")
        private List<BigDecimal> values;
    }

    /**
     * 对比分析
     */
    @Data
    @Schema(description = "对比分析")
    public static class CompareAnalysis {

        @Schema(description = "分组名称列表")
        private List<String> groups;

        @Schema(description = "指标列表")
        private List<String> indicators;

        @Schema(description = "数据 Map<指标名, List<各组数据>>")
        private Map<String, List<BigDecimal>> data;
    }
}
