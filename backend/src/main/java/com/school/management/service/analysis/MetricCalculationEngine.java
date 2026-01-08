package com.school.management.service.analysis;

import com.school.management.dto.analysis.AnalysisResultDTO.*;
import com.school.management.entity.analysis.AnalysisMetric;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.service.analysis.AnalysisExecutionService.AnalysisContext;
import com.school.management.service.analysis.AnalysisExecutionService.ClassAggregation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标计算引擎
 * 支持多种指标类型的计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricCalculationEngine {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 计算指标
     */
    public MetricResultDTO calculate(AnalysisMetric metric, AnalysisContext context) {
        String metricType = metric.getMetricType();

        MetricResultDTO result = MetricResultDTO.builder()
                .metricId(metric.getId())
                .metricCode(metric.getMetricCode())
                .metricName(metric.getMetricName())
                .metricType(metricType)
                .chartType(metric.getChartType())
                .chartConfig(metric.getChartConfig())
                .gridPosition(metric.getGridPosition())
                .build();

        Object data = null;

        switch (metricType) {
            case "total_score":
                data = calculateTotalScore(metric, context);
                break;
            case "avg_score":
                data = calculateAvgScore(metric, context);
                break;
            case "check_count":
                data = calculateCheckCount(metric, context);
                break;
            case "coverage_rate":
                data = calculateCoverageRate(metric, context);
                break;
            case "item_count":
                data = calculateItemCount(metric, context);
                break;
            case "ranking":
                data = calculateRanking(metric, context);
                break;
            case "trend":
                data = calculateTrend(metric, context);
                break;
            case "distribution":
                data = calculateDistribution(metric, context);
                break;
            case "overview":
                data = calculateOverview(metric, context);
                break;
            case "class_ranking":
                data = calculateClassRanking(metric, context);
                break;
            case "category_distribution":
                data = calculateCategoryDistribution(metric, context);
                break;
            case "grade_comparison":
                data = calculateGradeComparison(metric, context);
                break;
            case "department_comparison":
                data = calculateDepartmentComparison(metric, context);
                break;
            default:
                log.warn("不支持的指标类型: {}", metricType);
                return null;
        }

        result.setData(data);
        return result;
    }

    // ==================== 指标计算方法 ====================

    /**
     * 计算总扣分
     */
    private Object calculateTotalScore(AnalysisMetric metric, AnalysisContext context) {
        BigDecimal total = context.getClassAggregations().stream()
                .filter(a -> !a.isExcluded())
                .map(ClassAggregation::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return formatValue(total, metric);
    }

    /**
     * 计算平均扣分
     */
    private Object calculateAvgScore(AnalysisMetric metric, AnalysisContext context) {
        List<ClassAggregation> included = context.getClassAggregations().stream()
                .filter(a -> !a.isExcluded())
                .collect(Collectors.toList());

        if (included.isEmpty()) {
            return formatValue(BigDecimal.ZERO, metric);
        }

        BigDecimal total = included.stream()
                .map(ClassAggregation::getWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg = total.divide(BigDecimal.valueOf(included.size()), 2, RoundingMode.HALF_UP);
        return formatValue(avg, metric);
    }

    /**
     * 计算检查次数
     */
    private Object calculateCheckCount(AnalysisMetric metric, AnalysisContext context) {
        return context.getTotalRecordCount();
    }

    /**
     * 计算平均覆盖率
     */
    private Object calculateCoverageRate(AnalysisMetric metric, AnalysisContext context) {
        List<ClassAggregation> included = context.getClassAggregations().stream()
                .filter(a -> !a.isExcluded())
                .collect(Collectors.toList());

        if (included.isEmpty()) {
            return formatValue(BigDecimal.ZERO, metric);
        }

        BigDecimal avgCoverage = included.stream()
                .map(ClassAggregation::getCoverageRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(included.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return formatValue(avgCoverage, metric);
    }

    /**
     * 计算扣分项数
     */
    private Object calculateItemCount(AnalysisMetric metric, AnalysisContext context) {
        int total = context.getClassAggregations().stream()
                .filter(a -> !a.isExcluded())
                .mapToInt(ClassAggregation::getTotalItems)
                .sum();

        return total;
    }

    /**
     * 计算排名数据
     */
    private Object calculateRanking(AnalysisMetric metric, AnalysisContext context) {
        return calculateClassRanking(metric, context);
    }

    /**
     * 计算趋势数据
     */
    private List<TrendPointDTO> calculateTrend(AnalysisMetric metric, AnalysisContext context) {
        List<CheckRecordNew> records = context.getRecords();
        List<CheckRecordClassStatsNew> classStats = context.getClassStats();

        // 按日期分组
        String groupBy = metric.getGroupBy();
        if (groupBy == null) {
            groupBy = "date";
        }

        Map<String, List<CheckRecordNew>> recordsByPeriod;
        Map<String, List<CheckRecordClassStatsNew>> statsByPeriod;

        switch (groupBy) {
            case "week":
                recordsByPeriod = groupRecordsByWeek(records);
                statsByPeriod = groupStatsByWeek(classStats, records);
                break;
            case "month":
                recordsByPeriod = groupRecordsByMonth(records);
                statsByPeriod = groupStatsByMonth(classStats, records);
                break;
            default:
                recordsByPeriod = groupRecordsByDate(records);
                statsByPeriod = groupStatsByDate(classStats, records);
        }

        List<TrendPointDTO> trend = new ArrayList<>();
        List<String> sortedKeys = new ArrayList<>(recordsByPeriod.keySet());
        Collections.sort(sortedKeys);

        for (String period : sortedKeys) {
            List<CheckRecordNew> periodRecords = recordsByPeriod.get(period);
            List<CheckRecordClassStatsNew> periodStats = statsByPeriod.getOrDefault(period, Collections.emptyList());

            TrendPointDTO point = new TrendPointDTO();
            point.setDateLabel(period);

            if (!periodRecords.isEmpty()) {
                point.setDate(periodRecords.get(0).getCheckDate());
            }

            point.setCheckCount(periodRecords.size());

            // 计算扣分统计
            BigDecimal totalScore = periodStats.stream()
                    .map(CheckRecordClassStatsNew::getTotalScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            point.setTotalScore(totalScore);

            Set<Long> classIds = periodStats.stream()
                    .map(CheckRecordClassStatsNew::getClassId)
                    .collect(Collectors.toSet());
            point.setClassCount(classIds.size());

            BigDecimal avgScore = classIds.isEmpty()
                    ? BigDecimal.ZERO
                    : totalScore.divide(BigDecimal.valueOf(classIds.size()), 2, RoundingMode.HALF_UP);
            point.setAvgScore(avgScore);

            trend.add(point);
        }

        // 限制数量
        if (metric.getTopN() != null && metric.getTopN() > 0 && trend.size() > metric.getTopN()) {
            trend = trend.subList(trend.size() - metric.getTopN(), trend.size());
        }

        return trend;
    }

    /**
     * 计算分布数据
     */
    private List<DistributionItemDTO> calculateDistribution(AnalysisMetric metric, AnalysisContext context) {
        String groupBy = metric.getGroupBy();
        if (groupBy == null) {
            groupBy = "grade";
        }

        List<ClassAggregation> included = context.getClassAggregations().stream()
                .filter(a -> !a.isExcluded())
                .collect(Collectors.toList());

        Map<String, List<ClassAggregation>> grouped;

        switch (groupBy) {
            case "department":
                grouped = included.stream()
                        .collect(Collectors.groupingBy(ClassAggregation::getDepartmentName));
                break;
            case "grade":
            default:
                grouped = included.stream()
                        .collect(Collectors.groupingBy(ClassAggregation::getGradeName));
                break;
        }

        BigDecimal total = included.stream()
                .map(ClassAggregation::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DistributionItemDTO> distribution = new ArrayList<>();

        for (Map.Entry<String, List<ClassAggregation>> entry : grouped.entrySet()) {
            BigDecimal groupScore = entry.getValue().stream()
                    .map(ClassAggregation::getTotalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                    ? groupScore.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            distribution.add(DistributionItemDTO.builder()
                    .name(entry.getKey())
                    .value(groupScore)
                    .percentage(percentage)
                    .count(entry.getValue().size())
                    .build());
        }

        // 按值排序
        String sortOrder = metric.getSortOrder();
        if ("desc".equals(sortOrder)) {
            distribution.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        } else {
            distribution.sort(Comparator.comparing(DistributionItemDTO::getValue));
        }

        return distribution;
    }

    /**
     * 计算概览数据
     */
    private Object calculateOverview(AnalysisMetric metric, AnalysisContext context) {
        List<ClassAggregation> included = context.getClassAggregations().stream()
                .filter(a -> !a.isExcluded())
                .collect(Collectors.toList());

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("recordCount", context.getTotalRecordCount());
        overview.put("classCount", included.size());

        BigDecimal totalScore = included.stream()
                .map(ClassAggregation::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalScore", totalScore);

        BigDecimal avgScore = included.isEmpty()
                ? BigDecimal.ZERO
                : totalScore.divide(BigDecimal.valueOf(included.size()), 2, RoundingMode.HALF_UP);
        overview.put("avgScore", avgScore);

        int totalItems = included.stream()
                .mapToInt(ClassAggregation::getTotalItems)
                .sum();
        overview.put("totalItems", totalItems);

        long fullCoverage = included.stream()
                .filter(a -> a.getCoverageRate().compareTo(BigDecimal.ONE) >= 0)
                .count();
        overview.put("fullCoverageCount", fullCoverage);

        return overview;
    }

    /**
     * 计算班级排名
     */
    private List<ClassRankingItemDTO> calculateClassRanking(AnalysisMetric metric, AnalysisContext context) {
        List<ClassAggregation> included = context.getClassAggregations().stream()
                .filter(a -> !a.isExcluded())
                .collect(Collectors.toList());

        // 计算平均值
        BigDecimal avgScore = included.isEmpty()
                ? BigDecimal.ZERO
                : included.stream()
                .map(ClassAggregation::getWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(included.size()), 2, RoundingMode.HALF_UP);

        List<ClassRankingItemDTO> rankings = new ArrayList<>();

        for (ClassAggregation agg : included) {
            ClassRankingItemDTO item = ClassRankingItemDTO.builder()
                    .classId(agg.getClassId())
                    .className(agg.getClassName())
                    .gradeId(agg.getGradeId())
                    .gradeName(agg.getGradeName())
                    .departmentId(agg.getDepartmentId())
                    .departmentName(agg.getDepartmentName())
                    .teacherName(agg.getTeacherName())
                    .checkCount(agg.getCheckCount())
                    .expectedCheckCount(agg.getExpectedCheckCount())
                    .coverageRate(agg.getCoverageRate().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .totalScore(agg.getTotalScore())
                    .avgScore(agg.getAvgScore())
                    .weightedScore(agg.getWeightedScore())
                    .ranking(agg.getOverallRanking())
                    .gradeRanking(agg.getGradeRanking())
                    .vsAvg(agg.getWeightedScore().subtract(avgScore).setScale(2, RoundingMode.HALF_UP))
                    .scoreLevel(calculateScoreLevel(agg.getWeightedScore()))
                    .build();

            rankings.add(item);
        }

        // 排序
        String sortOrder = metric.getSortOrder();
        if ("desc".equals(sortOrder)) {
            rankings.sort((a, b) -> b.getWeightedScore().compareTo(a.getWeightedScore()));
        } else {
            rankings.sort(Comparator.comparing(ClassRankingItemDTO::getWeightedScore));
        }

        // 限制数量
        if (metric.getTopN() != null && metric.getTopN() > 0 && rankings.size() > metric.getTopN()) {
            rankings = rankings.subList(0, metric.getTopN());
        }

        return rankings;
    }

    /**
     * 计算类别分布
     */
    private List<DistributionItemDTO> calculateCategoryDistribution(AnalysisMetric metric, AnalysisContext context) {
        List<CheckRecordClassStatsNew> stats = context.getClassStats();

        // 按类别聚合
        Map<String, BigDecimal> categoryScores = new LinkedHashMap<>();
        categoryScores.put("卫生类", BigDecimal.ZERO);
        categoryScores.put("纪律类", BigDecimal.ZERO);
        categoryScores.put("考勤类", BigDecimal.ZERO);
        categoryScores.put("宿舍类", BigDecimal.ZERO);
        categoryScores.put("其他类", BigDecimal.ZERO);

        for (CheckRecordClassStatsNew stat : stats) {
            if (stat.getHygieneScore() != null) {
                categoryScores.merge("卫生类", stat.getHygieneScore(), BigDecimal::add);
            }
            if (stat.getDisciplineScore() != null) {
                categoryScores.merge("纪律类", stat.getDisciplineScore(), BigDecimal::add);
            }
            if (stat.getAttendanceScore() != null) {
                categoryScores.merge("考勤类", stat.getAttendanceScore(), BigDecimal::add);
            }
            if (stat.getDormitoryScore() != null) {
                categoryScores.merge("宿舍类", stat.getDormitoryScore(), BigDecimal::add);
            }
            if (stat.getOtherScore() != null) {
                categoryScores.merge("其他类", stat.getOtherScore(), BigDecimal::add);
            }
        }

        BigDecimal total = categoryScores.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DistributionItemDTO> distribution = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : categoryScores.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                        ? entry.getValue().divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                distribution.add(DistributionItemDTO.builder()
                        .name(entry.getKey())
                        .value(entry.getValue())
                        .percentage(percentage)
                        .build());
            }
        }

        distribution.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        return distribution;
    }

    /**
     * 计算年级对比
     */
    private List<DistributionItemDTO> calculateGradeComparison(AnalysisMetric metric, AnalysisContext context) {
        metric.setGroupBy("grade");
        return calculateDistribution(metric, context);
    }

    /**
     * 计算院系对比
     */
    private List<DistributionItemDTO> calculateDepartmentComparison(AnalysisMetric metric, AnalysisContext context) {
        metric.setGroupBy("department");
        return calculateDistribution(metric, context);
    }

    // ==================== 辅助方法 ====================

    private String formatValue(BigDecimal value, AnalysisMetric metric) {
        int decimalPlaces = metric.getDecimalPlaces() != null ? metric.getDecimalPlaces() : 2;
        BigDecimal formatted = value.setScale(decimalPlaces, RoundingMode.HALF_UP);

        String format = metric.getDisplayFormat();
        if (format != null && format.contains("{value}")) {
            return format.replace("{value}", formatted.toString());
        }

        String unit = metric.getUnit();
        if (unit != null && !unit.isEmpty()) {
            return formatted + unit;
        }

        return formatted.toString();
    }

    private String calculateScoreLevel(BigDecimal score) {
        if (score == null) {
            return "未评级";
        }

        // 扣分越少越好
        if (score.compareTo(BigDecimal.valueOf(2)) <= 0) {
            return "优秀";
        } else if (score.compareTo(BigDecimal.valueOf(5)) <= 0) {
            return "良好";
        } else if (score.compareTo(BigDecimal.valueOf(10)) <= 0) {
            return "一般";
        } else {
            return "较差";
        }
    }

    // ==================== 分组辅助方法 ====================

    private Map<String, List<CheckRecordNew>> groupRecordsByDate(List<CheckRecordNew> records) {
        return records.stream()
                .filter(r -> r.getCheckDate() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCheckDate().format(DATE_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<String, List<CheckRecordNew>> groupRecordsByWeek(List<CheckRecordNew> records) {
        // 按周分组，使用周一作为周的开始
        return records.stream()
                .filter(r -> r.getCheckDate() != null)
                .collect(Collectors.groupingBy(
                        r -> getWeekLabel(r.getCheckDate()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<String, List<CheckRecordNew>> groupRecordsByMonth(List<CheckRecordNew> records) {
        return records.stream()
                .filter(r -> r.getCheckDate() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCheckDate().format(MONTH_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<String, List<CheckRecordClassStatsNew>> groupStatsByDate(
            List<CheckRecordClassStatsNew> stats, List<CheckRecordNew> records) {
        Map<Long, LocalDate> recordDateMap = records.stream()
                .collect(Collectors.toMap(CheckRecordNew::getId, CheckRecordNew::getCheckDate, (a, b) -> a));

        return stats.stream()
                .filter(s -> recordDateMap.containsKey(s.getRecordId()))
                .collect(Collectors.groupingBy(
                        s -> recordDateMap.get(s.getRecordId()).format(DATE_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<String, List<CheckRecordClassStatsNew>> groupStatsByWeek(
            List<CheckRecordClassStatsNew> stats, List<CheckRecordNew> records) {
        Map<Long, LocalDate> recordDateMap = records.stream()
                .collect(Collectors.toMap(CheckRecordNew::getId, CheckRecordNew::getCheckDate, (a, b) -> a));

        return stats.stream()
                .filter(s -> recordDateMap.containsKey(s.getRecordId()))
                .collect(Collectors.groupingBy(
                        s -> getWeekLabel(recordDateMap.get(s.getRecordId())),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<String, List<CheckRecordClassStatsNew>> groupStatsByMonth(
            List<CheckRecordClassStatsNew> stats, List<CheckRecordNew> records) {
        Map<Long, LocalDate> recordDateMap = records.stream()
                .collect(Collectors.toMap(CheckRecordNew::getId, CheckRecordNew::getCheckDate, (a, b) -> a));

        return stats.stream()
                .filter(s -> recordDateMap.containsKey(s.getRecordId()))
                .collect(Collectors.groupingBy(
                        s -> recordDateMap.get(s.getRecordId()).format(MONTH_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private String getWeekLabel(LocalDate date) {
        // 获取该日期所在周的周一
        LocalDate monday = date.minusDays(date.getDayOfWeek().getValue() - 1);
        return monday.format(DATE_FORMATTER) + "周";
    }
}
