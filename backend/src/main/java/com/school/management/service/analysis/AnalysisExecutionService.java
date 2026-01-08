package com.school.management.service.analysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.analysis.AnalysisConfigDTO;
import com.school.management.dto.analysis.AnalysisMetricDTO;
import com.school.management.dto.analysis.AnalysisResultDTO;
import com.school.management.dto.analysis.AnalysisResultDTO.*;
import com.school.management.entity.analysis.StatAnalysisConfig;
import com.school.management.entity.analysis.AnalysisMetric;
import com.school.management.entity.analysis.AnalysisSnapshot;
import com.school.management.entity.analysis.CategoryMapping;
import com.school.management.entity.record.CheckRecordCategoryStatsNew;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.analysis.StatAnalysisConfigMapper;
import com.school.management.mapper.analysis.AnalysisMetricMapper;
import com.school.management.mapper.analysis.AnalysisSnapshotMapper;
import com.school.management.mapper.analysis.CategoryMappingMapper;
import com.school.management.mapper.record.CheckRecordClassStatsMapper;
import com.school.management.mapper.record.CheckRecordMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分析执行引擎
 * 核心职责：
 * 1. 加载配置
 * 2. 确定分析范围（时间范围或记录选择）
 * 3. 获取检查记录和统计数据
 * 4. 计算覆盖率和加权分数
 * 5. 处理缺失数据策略
 * 6. 调用指标计算引擎
 * 7. 生成并保存分析结果
 */
@Slf4j
@Service
public class AnalysisExecutionService {

    private final StatAnalysisConfigMapper configMapper;
    private final AnalysisMetricMapper metricMapper;
    private final AnalysisSnapshotMapper snapshotMapper;
    private final CategoryMappingMapper categoryMappingMapper;
    private final CheckRecordMapper recordMapper;
    private final CheckRecordClassStatsMapper classStatsMapper;
    private final MetricCalculationEngine metricCalculationEngine;
    private final ObjectMapper objectMapper;

    public AnalysisExecutionService(
            StatAnalysisConfigMapper configMapper,
            AnalysisMetricMapper metricMapper,
            AnalysisSnapshotMapper snapshotMapper,
            CategoryMappingMapper categoryMappingMapper,
            @Qualifier("newCheckRecordMapper") CheckRecordMapper recordMapper,
            @Qualifier("newCheckRecordClassStatsMapper") CheckRecordClassStatsMapper classStatsMapper,
            MetricCalculationEngine metricCalculationEngine,
            ObjectMapper objectMapper) {
        this.configMapper = configMapper;
        this.metricMapper = metricMapper;
        this.snapshotMapper = snapshotMapper;
        this.categoryMappingMapper = categoryMappingMapper;
        this.recordMapper = recordMapper;
        this.classStatsMapper = classStatsMapper;
        this.metricCalculationEngine = metricCalculationEngine;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行分析
     * @param configId 配置ID
     * @param saveSnapshot 是否保存快照
     * @return 分析结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AnalysisResultDTO executeAnalysis(Long configId, boolean saveSnapshot) {
        log.info("开始执行分析，配置ID: {}", configId);

        // 1. 加载配置
        StatAnalysisConfig config = loadConfig(configId);
        List<AnalysisMetric> metrics = metricMapper.selectEnabledByConfigId(configId);
        List<CategoryMapping> categoryMappings = categoryMappingMapper.selectByConfigId(configId);

        // 2. 确定分析范围
        AnalysisScope scope = determineScope(config);
        log.info("分析范围：{} - {}, 记录数: {}", scope.getStartDate(), scope.getEndDate(),
                scope.getRecordIds() != null ? scope.getRecordIds().size() : "全部");

        // 3. 获取检查记录
        List<CheckRecordNew> records = fetchRecords(config.getPlanId(), scope);
        if (CollectionUtils.isEmpty(records)) {
            throw new BusinessException("指定范围内没有检查记录");
        }
        log.info("获取到 {} 条检查记录", records.size());

        // 4. 获取班级统计数据
        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());
        List<CheckRecordClassStatsNew> allClassStats = fetchClassStats(recordIds);

        // 5. 应用目标过滤
        List<CheckRecordClassStatsNew> filteredStats = applyTargetFilter(allClassStats, config);
        log.info("过滤后班级统计记录数: {}", filteredStats.size());

        // 6. 计算覆盖率和聚合数据
        AnalysisContext context = buildAnalysisContext(config, records, filteredStats, categoryMappings);

        // 7. 计算各指标
        List<MetricResultDTO> metricResults = calculateMetrics(metrics, context);

        // 8. 构建结果
        AnalysisResultDTO result = buildResult(config, scope, context, metricResults);

        // 9. 保存快照
        if (saveSnapshot) {
            saveSnapshot(config, result);
        }

        log.info("分析执行完成，配置ID: {}", configId);
        return result;
    }

    /**
     * 获取历史快照
     */
    public AnalysisResultDTO getSnapshot(Long snapshotId) {
        AnalysisSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BusinessException("快照不存在");
        }

        try {
            return objectMapper.readValue(snapshot.getResultData(), AnalysisResultDTO.class);
        } catch (Exception e) {
            log.error("解析快照数据失败", e);
            throw new BusinessException("解析快照数据失败");
        }
    }

    /**
     * 获取最新快照
     */
    public AnalysisResultDTO getLatestSnapshot(Long configId) {
        AnalysisSnapshot snapshot = snapshotMapper.selectLatestByConfigId(configId);
        if (snapshot == null) {
            return null;
        }

        try {
            return objectMapper.readValue(snapshot.getResultData(), AnalysisResultDTO.class);
        } catch (Exception e) {
            log.error("解析快照数据失败", e);
            throw new BusinessException("解析快照数据失败");
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 加载配置
     */
    private StatAnalysisConfig loadConfig(Long configId) {
        StatAnalysisConfig config = configMapper.selectById(configId);
        if (config == null || Boolean.TRUE.equals(config.getDeleted())) {
            throw new BusinessException("配置不存在");
        }
        if (!Boolean.TRUE.equals(config.getIsEnabled())) {
            throw new BusinessException("配置未启用");
        }
        return config;
    }

    /**
     * 确定分析范围
     */
    private AnalysisScope determineScope(StatAnalysisConfig config) {
        AnalysisScope scope = new AnalysisScope();
        Map<String, Object> scopeConfig = config.getScopeConfig();

        String scopeType = config.getScopeType();
        if ("time".equals(scopeType) || "mixed".equals(scopeType)) {
            // 时间范围模式
            if (scopeConfig != null) {
                if (scopeConfig.get("startDate") != null) {
                    scope.setStartDate(LocalDate.parse(scopeConfig.get("startDate").toString()));
                }
                if (scopeConfig.get("endDate") != null) {
                    scope.setEndDate(LocalDate.parse(scopeConfig.get("endDate").toString()));
                }
            }

            // 动态更新模式：结束日期为今天
            if ("dynamic".equals(config.getUpdateMode())) {
                scope.setEndDate(LocalDate.now());
            }
        }

        if ("record".equals(scopeType) || "mixed".equals(scopeType)) {
            // 记录选择模式
            if (scopeConfig != null && scopeConfig.get("recordIds") != null) {
                try {
                    List<Long> recordIds = objectMapper.convertValue(
                            scopeConfig.get("recordIds"),
                            new TypeReference<List<Long>>() {}
                    );
                    scope.setRecordIds(recordIds);
                } catch (Exception e) {
                    log.warn("解析recordIds失败", e);
                }
            }
        }

        return scope;
    }

    /**
     * 获取检查记录
     */
    private List<CheckRecordNew> fetchRecords(Long planId, AnalysisScope scope) {
        LambdaQueryWrapper<CheckRecordNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRecordNew::getPlanId, planId)
                .eq(CheckRecordNew::getDeleted, 0)
                .eq(CheckRecordNew::getStatus, CheckRecordNew.STATUS_PUBLISHED);

        // 按记录ID筛选
        if (!CollectionUtils.isEmpty(scope.getRecordIds())) {
            wrapper.in(CheckRecordNew::getId, scope.getRecordIds());
        }

        // 按时间范围筛选
        if (scope.getStartDate() != null) {
            wrapper.ge(CheckRecordNew::getCheckDate, scope.getStartDate());
        }
        if (scope.getEndDate() != null) {
            wrapper.le(CheckRecordNew::getCheckDate, scope.getEndDate());
        }

        wrapper.orderByAsc(CheckRecordNew::getCheckDate);

        return recordMapper.selectList(wrapper);
    }

    /**
     * 获取班级统计数据
     */
    private List<CheckRecordClassStatsNew> fetchClassStats(List<Long> recordIds) {
        if (CollectionUtils.isEmpty(recordIds)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<CheckRecordClassStatsNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CheckRecordClassStatsNew::getRecordId, recordIds);

        return classStatsMapper.selectList(wrapper);
    }

    /**
     * 应用目标过滤
     */
    private List<CheckRecordClassStatsNew> applyTargetFilter(List<CheckRecordClassStatsNew> stats, StatAnalysisConfig config) {
        String targetType = config.getTargetType();
        Map<String, Object> targetConfig = config.getTargetConfig();

        if ("all".equals(targetType) || targetConfig == null) {
            return stats;
        }

        List<Long> targetIds = null;
        try {
            if (targetConfig.get("ids") != null) {
                targetIds = objectMapper.convertValue(
                        targetConfig.get("ids"),
                        new TypeReference<List<Long>>() {}
                );
            } else if (targetConfig.get("departmentIds") != null) {
                targetIds = objectMapper.convertValue(
                        targetConfig.get("departmentIds"),
                        new TypeReference<List<Long>>() {}
                );
            } else if (targetConfig.get("gradeIds") != null) {
                targetIds = objectMapper.convertValue(
                        targetConfig.get("gradeIds"),
                        new TypeReference<List<Long>>() {}
                );
            } else if (targetConfig.get("classIds") != null) {
                targetIds = objectMapper.convertValue(
                        targetConfig.get("classIds"),
                        new TypeReference<List<Long>>() {}
                );
            }
        } catch (Exception e) {
            log.warn("解析目标过滤配置失败", e);
        }

        if (CollectionUtils.isEmpty(targetIds)) {
            return stats;
        }

        final List<Long> finalTargetIds = targetIds;

        switch (targetType) {
            case "department":
                return stats.stream()
                        .filter(s -> finalTargetIds.contains(s.getDepartmentId()))
                        .collect(Collectors.toList());
            case "grade":
                return stats.stream()
                        .filter(s -> finalTargetIds.contains(s.getGradeId()))
                        .collect(Collectors.toList());
            case "custom":
                return stats.stream()
                        .filter(s -> finalTargetIds.contains(s.getClassId()))
                        .collect(Collectors.toList());
            default:
                return stats;
        }
    }

    /**
     * 构建分析上下文
     */
    private AnalysisContext buildAnalysisContext(
            StatAnalysisConfig config,
            List<CheckRecordNew> records,
            List<CheckRecordClassStatsNew> classStats,
            List<CategoryMapping> categoryMappings) {

        AnalysisContext context = new AnalysisContext();
        context.setConfig(config);
        context.setRecords(records);
        context.setClassStats(classStats);
        context.setCategoryMappings(categoryMappings);

        int totalRecords = records.size();
        context.setTotalRecordCount(totalRecords);

        // 构建班级映射：classId -> List<ClassStats>
        Map<Long, List<CheckRecordClassStatsNew>> classStatsMap = classStats.stream()
                .collect(Collectors.groupingBy(CheckRecordClassStatsNew::getClassId));
        context.setClassStatsMap(classStatsMap);

        // 计算每个班级的覆盖率和聚合数据
        List<ClassAggregation> classAggregations = new ArrayList<>();
        Set<Long> allClassIds = classStatsMap.keySet();

        for (Long classId : allClassIds) {
            List<CheckRecordClassStatsNew> classRecords = classStatsMap.get(classId);
            ClassAggregation agg = aggregateClassData(classId, classRecords, totalRecords, config);
            classAggregations.add(agg);
        }

        // 应用缺失数据策略
        applyMissingStrategy(classAggregations, config, totalRecords);

        // 计算排名
        calculateRankings(classAggregations);

        context.setClassAggregations(classAggregations);

        return context;
    }

    /**
     * 聚合班级数据
     */
    private ClassAggregation aggregateClassData(
            Long classId,
            List<CheckRecordClassStatsNew> classRecords,
            int totalRecords,
            StatAnalysisConfig config) {

        ClassAggregation agg = new ClassAggregation();
        agg.setClassId(classId);

        if (!classRecords.isEmpty()) {
            CheckRecordClassStatsNew first = classRecords.get(0);
            agg.setClassName(first.getClassName());
            agg.setGradeId(first.getGradeId());
            agg.setGradeName(first.getGradeName());
            agg.setDepartmentId(first.getDepartmentId());
            agg.setDepartmentName(first.getDepartmentName());
            agg.setTeacherName(first.getTeacherName());
        }

        // 计算检查次数和覆盖率
        int checkCount = classRecords.size();
        agg.setCheckCount(checkCount);
        agg.setExpectedCheckCount(totalRecords);

        BigDecimal coverageRate = totalRecords > 0
                ? BigDecimal.valueOf(checkCount).divide(BigDecimal.valueOf(totalRecords), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        agg.setCoverageRate(coverageRate);

        // 计算总扣分和平均扣分
        BigDecimal totalScore = classRecords.stream()
                .map(CheckRecordClassStatsNew::getTotalScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        agg.setTotalScore(totalScore);

        BigDecimal avgScore = checkCount > 0
                ? totalScore.divide(BigDecimal.valueOf(checkCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        agg.setAvgScore(avgScore);

        // 计算扣分项总数
        int totalItems = classRecords.stream()
                .mapToInt(r -> r.getDeductionCount() != null ? r.getDeductionCount() : 0)
                .sum();
        agg.setTotalItems(totalItems);

        return agg;
    }

    /**
     * 应用缺失数据策略
     */
    private void applyMissingStrategy(List<ClassAggregation> aggregations, StatAnalysisConfig config, int totalRecords) {
        String strategy = config.getMissingStrategy();
        if (strategy == null) {
            strategy = "avg";
        }

        // 计算全局平均值
        BigDecimal globalAvgScore = aggregations.stream()
                .filter(a -> a.getCheckCount() > 0)
                .map(ClassAggregation::getAvgScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long classesWithData = aggregations.stream()
                .filter(a -> a.getCheckCount() > 0)
                .count();

        BigDecimal overallAvg = classesWithData > 0
                ? globalAvgScore.divide(BigDecimal.valueOf(classesWithData), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        for (ClassAggregation agg : aggregations) {
            if (agg.getCoverageRate().compareTo(BigDecimal.ONE) >= 0) {
                // 完全覆盖，加权分数 = 平均分
                agg.setWeightedScore(agg.getAvgScore());
                agg.setMissingFilled(false);
                continue;
            }

            BigDecimal missingRate = BigDecimal.ONE.subtract(agg.getCoverageRate());
            BigDecimal weightedScore;

            switch (strategy) {
                case "avg":
                    // 缺失部分用全局平均值填充
                    weightedScore = agg.getAvgScore().multiply(agg.getCoverageRate())
                            .add(overallAvg.multiply(missingRate));
                    agg.setWeightedScore(weightedScore.setScale(2, RoundingMode.HALF_UP));
                    agg.setMissingFilled(true);
                    break;

                case "weighted":
                    // 仅用已有数据，但加权计算
                    agg.setWeightedScore(agg.getAvgScore());
                    agg.setMissingFilled(false);
                    break;

                case "full_only":
                    // 只统计完全覆盖的班级
                    if (agg.getCheckCount() < totalRecords) {
                        agg.setExcluded(true);
                    }
                    agg.setWeightedScore(agg.getAvgScore());
                    agg.setMissingFilled(false);
                    break;

                case "penalty":
                    // 惩罚策略：缺失部分用配置的惩罚分填充
                    BigDecimal penaltyScore = BigDecimal.ZERO;
                    if (config.getMissingStrategyConfig() != null &&
                            config.getMissingStrategyConfig().get("penaltyScore") != null) {
                        penaltyScore = new BigDecimal(config.getMissingStrategyConfig().get("penaltyScore").toString());
                    }
                    weightedScore = agg.getAvgScore().multiply(agg.getCoverageRate())
                            .add(penaltyScore.multiply(missingRate));
                    agg.setWeightedScore(weightedScore.setScale(2, RoundingMode.HALF_UP));
                    agg.setMissingFilled(true);
                    break;

                case "exempt":
                    // 豁免策略：忽略缺失，直接用平均值
                    agg.setWeightedScore(agg.getAvgScore());
                    agg.setMissingFilled(false);
                    break;

                default:
                    agg.setWeightedScore(agg.getAvgScore());
                    agg.setMissingFilled(false);
            }
        }
    }

    /**
     * 计算排名
     */
    private void calculateRankings(List<ClassAggregation> aggregations) {
        // 过滤出未排除的班级
        List<ClassAggregation> includedClasses = aggregations.stream()
                .filter(a -> !a.isExcluded())
                .collect(Collectors.toList());

        // 按加权分数排序（扣分越少排名越前）
        includedClasses.sort(Comparator.comparing(ClassAggregation::getWeightedScore));

        // 分配全校排名
        int rank = 1;
        for (ClassAggregation agg : includedClasses) {
            agg.setOverallRanking(rank++);
        }

        // 按年级分组计算年级排名
        Map<Long, List<ClassAggregation>> byGrade = includedClasses.stream()
                .collect(Collectors.groupingBy(ClassAggregation::getGradeId));

        for (List<ClassAggregation> gradeClasses : byGrade.values()) {
            int gradeRank = 1;
            for (ClassAggregation agg : gradeClasses) {
                agg.setGradeRanking(gradeRank++);
            }
        }

        // 按院系分组计算院系排名
        Map<Long, List<ClassAggregation>> byDept = includedClasses.stream()
                .collect(Collectors.groupingBy(ClassAggregation::getDepartmentId));

        for (List<ClassAggregation> deptClasses : byDept.values()) {
            int deptRank = 1;
            for (ClassAggregation agg : deptClasses) {
                agg.setDepartmentRanking(deptRank++);
            }
        }
    }

    /**
     * 计算各指标
     */
    private List<MetricResultDTO> calculateMetrics(List<AnalysisMetric> metrics, AnalysisContext context) {
        List<MetricResultDTO> results = new ArrayList<>();

        for (AnalysisMetric metric : metrics) {
            try {
                MetricResultDTO result = metricCalculationEngine.calculate(metric, context);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("计算指标失败: {}", metric.getMetricCode(), e);
            }
        }

        return results;
    }

    /**
     * 构建结果
     */
    private AnalysisResultDTO buildResult(
            StatAnalysisConfig config,
            AnalysisScope scope,
            AnalysisContext context,
            List<MetricResultDTO> metricResults) {

        AnalysisResultDTO result = new AnalysisResultDTO();
        result.setConfigId(config.getId());
        result.setConfigName(config.getConfigName());
        result.setDateRangeStart(scope.getStartDate());
        result.setDateRangeEnd(scope.getEndDate());
        result.setGeneratedAt(LocalDateTime.now());
        result.setIsDynamic("dynamic".equals(config.getUpdateMode()));

        // 概览数据
        OverviewDTO overview = buildOverview(context);
        result.setOverview(overview);

        // 指标结果
        result.setMetricResults(metricResults);

        return result;
    }

    /**
     * 构建概览
     */
    private OverviewDTO buildOverview(AnalysisContext context) {
        OverviewDTO overview = new OverviewDTO();

        List<ClassAggregation> aggregations = context.getClassAggregations();
        List<ClassAggregation> included = aggregations.stream()
                .filter(a -> !a.isExcluded())
                .collect(Collectors.toList());

        overview.setRecordCount(context.getTotalRecordCount());
        overview.setClassCount(included.size());

        // 总扣分
        BigDecimal totalScore = included.stream()
                .map(ClassAggregation::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.setTotalScore(totalScore);

        // 平均扣分
        BigDecimal avgScore = included.isEmpty()
                ? BigDecimal.ZERO
                : totalScore.divide(BigDecimal.valueOf(included.size()), 2, RoundingMode.HALF_UP);
        overview.setAvgScore(avgScore);

        // 最高/最低扣分
        BigDecimal maxScore = included.stream()
                .map(ClassAggregation::getWeightedScore)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal minScore = included.stream()
                .map(ClassAggregation::getWeightedScore)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        overview.setMaxScore(maxScore);
        overview.setMinScore(minScore);

        // 扣分项总数
        int totalItems = included.stream()
                .mapToInt(ClassAggregation::getTotalItems)
                .sum();
        overview.setTotalItems(totalItems);

        // 平均覆盖率
        BigDecimal avgCoverage = included.isEmpty()
                ? BigDecimal.ZERO
                : included.stream()
                .map(ClassAggregation::getCoverageRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(included.size()), 4, RoundingMode.HALF_UP);
        overview.setAvgCoverageRate(avgCoverage);

        // 全覆盖班级数
        long fullCoverage = included.stream()
                .filter(a -> a.getCoverageRate().compareTo(BigDecimal.ONE) >= 0)
                .count();
        overview.setFullCoverageCount((int) fullCoverage);

        return overview;
    }

    /**
     * 保存快照
     */
    private void saveSnapshot(StatAnalysisConfig config, AnalysisResultDTO result) {
        try {
            AnalysisSnapshot snapshot = new AnalysisSnapshot();
            snapshot.setConfigId(config.getId());
            snapshot.setResultData(objectMapper.writeValueAsString(result));
            snapshot.setGeneratedAt(LocalDateTime.now());
            snapshot.setDateRangeStart(result.getDateRangeStart());
            snapshot.setDateRangeEnd(result.getDateRangeEnd());
            snapshot.setRecordCount(result.getOverview().getRecordCount());
            snapshot.setClassCount(result.getOverview().getClassCount());
            snapshot.setCreatedAt(LocalDateTime.now());

            snapshotMapper.insert(snapshot);
            log.info("保存分析快照成功，配置ID: {}", config.getId());
        } catch (Exception e) {
            log.error("保存分析快照失败", e);
        }
    }

    // ==================== 内部类 ====================

    /**
     * 分析范围
     */
    @Data
    public static class AnalysisScope {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<Long> recordIds;
    }

    /**
     * 分析上下文
     */
    @Data
    public static class AnalysisContext {
        private StatAnalysisConfig config;
        private List<CheckRecordNew> records;
        private List<CheckRecordClassStatsNew> classStats;
        private List<CategoryMapping> categoryMappings;
        private Map<Long, List<CheckRecordClassStatsNew>> classStatsMap;
        private List<ClassAggregation> classAggregations;
        private int totalRecordCount;
    }

    /**
     * 班级聚合数据
     */
    @Data
    public static class ClassAggregation {
        private Long classId;
        private String className;
        private Long gradeId;
        private String gradeName;
        private Long departmentId;
        private String departmentName;
        private String teacherName;

        private int checkCount;
        private int expectedCheckCount;
        private BigDecimal coverageRate;

        private BigDecimal totalScore;
        private BigDecimal avgScore;
        private BigDecimal weightedScore;
        private int totalItems;

        private Integer overallRanking;
        private Integer gradeRanking;
        private Integer departmentRanking;

        private boolean missingFilled;
        private boolean excluded;
    }
}
