package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.statistics.smart.*;
import com.school.management.entity.CheckPlan;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import com.school.management.entity.record.CheckRecordDeductionNew;
import com.school.management.mapper.CheckPlanMapper;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.record.CheckRecordMapper;
import com.school.management.mapper.record.CheckRecordClassStatsMapper;
import com.school.management.mapper.record.CheckRecordDeductionMapper;
import com.school.management.service.SmartStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能统计服务实现
 * 提供动态适应、多维度分析的统计功能
 *
 * 使用新版表结构：check_records_new, check_record_class_stats_new, check_record_deductions_new
 */
@Slf4j
@Service
public class SmartStatisticsServiceImpl implements SmartStatisticsService {

    private final CheckRecordMapper checkRecordMapper;
    private final CheckRecordClassStatsMapper classStatsMapper;
    private final CheckRecordDeductionMapper deductionMapper;
    private final CheckPlanMapper checkPlanMapper;
    private final ClassMapper classMapper;
    private final ObjectMapper objectMapper;

    public SmartStatisticsServiceImpl(
            @Qualifier("newCheckRecordMapper") CheckRecordMapper checkRecordMapper,
            @Qualifier("newCheckRecordClassStatsMapper") CheckRecordClassStatsMapper classStatsMapper,
            @Qualifier("newCheckRecordDeductionMapper") CheckRecordDeductionMapper deductionMapper,
            CheckPlanMapper checkPlanMapper,
            ClassMapper classMapper,
            ObjectMapper objectMapper) {
        this.checkRecordMapper = checkRecordMapper;
        this.classStatsMapper = classStatsMapper;
        this.deductionMapper = deductionMapper;
        this.checkPlanMapper = checkPlanMapper;
        this.classMapper = classMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public SmartStatisticsOverviewVO getSmartOverview(SmartStatisticsFilters filters) {
        Long planId = filters.getPlanId();

        // 获取检查计划
        CheckPlan plan = checkPlanMapper.selectById(planId);
        if (plan == null) {
            return null;
        }

        // 获取符合条件的检查记录
        List<CheckRecordNew> records = getFilteredRecords(filters);

        // 获取覆盖率信息
        CheckCoverageVO coverage = calculateCoverage(plan, records);

        // 如果没有记录，返回空概览
        if (records.isEmpty()) {
            return SmartStatisticsOverviewVO.builder()
                    .planId(planId)
                    .planName(plan.getPlanName())
                    .status(plan.getStatus())
                    .startDate(plan.getStartDate())
                    .endDate(plan.getEndDate())
                    .totalChecks(0)
                    .totalClasses(0)
                    .totalRounds(0)
                    .coverage(coverage)
                    .totalScore(BigDecimal.ZERO)
                    .avgScorePerCheck(BigDecimal.ZERO)
                    .avgScorePerRound(BigDecimal.ZERO)
                    .maxScore(BigDecimal.ZERO)
                    .minScore(BigDecimal.ZERO)
                    .totalItems(0)
                    .totalPersons(0)
                    .weightEnabled(false)
                    .warnings(Arrays.asList("暂无检查记录"))
                    .insights(Collections.emptyList())
                    .build();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 统计基础数据
        BigDecimal totalScore = BigDecimal.ZERO;
        Set<Long> allClassIds = new HashSet<>();
        int totalItems = 0;
        int totalPersons = 0;
        int totalRounds = 0;

        for (CheckRecordNew record : records) {
            totalScore = totalScore.add(record.getTotalDeductionScore() != null ? record.getTotalDeductionScore() : BigDecimal.ZERO);
        }

        // 获取班级统计
        List<CheckRecordClassStatsNew> allClassStats = classStatsMapper.selectList(
                new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                        .in(CheckRecordClassStatsNew::getRecordId, recordIds)
        );

        for (CheckRecordClassStatsNew cs : allClassStats) {
            allClassIds.add(cs.getClassId());
        }

        // 获取扣分明细统计
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .in(CheckRecordDeductionNew::getRecordId, recordIds)
        );

        totalItems = allDeductions.size();
        totalPersons = allDeductions.stream()
                .mapToInt(item -> item.getPersonCount() != null ? item.getPersonCount() : 0)
                .sum();

        // 计算总轮次
        totalRounds = allDeductions.stream()
                .mapToInt(item -> item.getCheckRound() != null ? item.getCheckRound() : 1)
                .max()
                .orElse(1) * records.size();

        // 计算平均分
        BigDecimal avgScorePerCheck = records.size() > 0
                ? totalScore.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgScorePerRound = totalRounds > 0
                ? totalScore.divide(BigDecimal.valueOf(totalRounds), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 最高/最低班级扣分
        BigDecimal maxScore = allClassStats.stream()
                .map(cs -> cs.getTotalScore() != null ? cs.getTotalScore() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minScore = allClassStats.stream()
                .map(cs -> cs.getTotalScore() != null ? cs.getTotalScore() : BigDecimal.ZERO)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 新版不支持加权（如果需要可以从模板配置中获取）
        boolean weightEnabled = false;

        // 获取最近检查日期
        LocalDate lastCheckDate = records.stream()
                .map(CheckRecordNew::getCheckDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        // 计算趋势
        SmartStatisticsOverviewVO.TrendInfo trend = calculateTrend(records, filters);

        // 生成警告
        List<String> warnings = generateWarnings(coverage, records, allClassStats);

        // 生成洞察（使用内部方法避免循环调用）
        List<String> insights = generateInsightsInternal(coverage, trend, filters);

        return SmartStatisticsOverviewVO.builder()
                .planId(planId)
                .planName(plan.getPlanName())
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .totalChecks(records.size())
                .totalClasses(allClassIds.size())
                .totalRounds(totalRounds)
                .coverage(coverage)
                .totalScore(totalScore)
                .avgScorePerCheck(avgScorePerCheck)
                .avgScorePerRound(avgScorePerRound)
                .maxScore(maxScore)
                .minScore(minScore)
                .totalItems(totalItems)
                .totalPersons(totalPersons)
                .trend(trend)
                .lastCheckDate(lastCheckDate)
                .weightEnabled(weightEnabled)
                .weightedTotalScore(null)
                .warnings(warnings)
                .insights(insights)
                .build();
    }

    @Override
    public SmartRankingResultVO getSmartRanking(SmartStatisticsFilters filters) {
        List<CheckRecordNew> records = getFilteredRecords(filters);

        if (records.isEmpty()) {
            return SmartRankingResultVO.builder()
                    .rankings(Collections.emptyList())
                    .total(0L)
                    .pageNum(filters.getPageNumOrDefault())
                    .pageSize(filters.getPageSizeOrDefault())
                    .warnings(Arrays.asList("暂无检查记录"))
                    .build();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 获取所有班级统计
        List<CheckRecordClassStatsNew> allClassStats = classStatsMapper.selectList(
                new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                        .in(CheckRecordClassStatsNew::getRecordId, recordIds)
        );

        // 应用筛选
        if (filters.getClassIds() != null && !filters.getClassIds().isEmpty()) {
            allClassStats = allClassStats.stream()
                    .filter(cs -> filters.getClassIds().contains(cs.getClassId()))
                    .collect(Collectors.toList());
        }
        if (filters.getGradeIds() != null && !filters.getGradeIds().isEmpty()) {
            allClassStats = allClassStats.stream()
                    .filter(cs -> filters.getGradeIds().contains(cs.getGradeId()))
                    .collect(Collectors.toList());
        }

        // 按班级聚合
        Map<Long, List<CheckRecordClassStatsNew>> groupedByClass = allClassStats.stream()
                .collect(Collectors.groupingBy(CheckRecordClassStatsNew::getClassId));

        // 获取扣分明细用于类别统计和轮次分析
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .in(CheckRecordDeductionNew::getRecordId, recordIds)
        );
        Map<Long, List<CheckRecordDeductionNew>> deductionsByClass = allDeductions.stream()
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getClassId));

        // 构建排名列表
        List<SmartClassRankingVO> rankings = new ArrayList<>();
        String compareMode = filters.getCompareModeOrDefault();

        for (Map.Entry<Long, List<CheckRecordClassStatsNew>> entry : groupedByClass.entrySet()) {
            Long classId = entry.getKey();
            List<CheckRecordClassStatsNew> stats = entry.getValue();
            CheckRecordClassStatsNew first = stats.get(0);

            // 计算多维度得分
            BigDecimal totalScore = stats.stream()
                    .map(s -> s.getTotalScore() != null ? s.getTotalScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 新版不支持加权，使用总分
            BigDecimal weightedScore = totalScore;

            int checkCount = stats.size();
            BigDecimal avgScorePerCheck = checkCount > 0
                    ? totalScore.divide(BigDecimal.valueOf(checkCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 计算轮次
            List<CheckRecordDeductionNew> classDeductions = deductionsByClass.getOrDefault(classId, Collections.emptyList());
            int totalRounds = classDeductions.stream()
                    .mapToInt(item -> item.getCheckRound() != null ? item.getCheckRound() : 1)
                    .max()
                    .orElse(1) * checkCount;

            BigDecimal avgScorePerRound = totalRounds > 0
                    ? totalScore.divide(BigDecimal.valueOf(totalRounds), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 类别得分明细（从扣分明细聚合）
            List<SmartClassRankingVO.CategoryScoreDetailVO> categoryScores = buildCategoryScoresFromDeductions(classDeductions, totalScore);

            // 轮次得分（如果有多轮）
            List<SmartClassRankingVO.RoundScoreVO> roundScores = buildRoundScoresFromDeductions(classDeductions);

            // 轮次间改善率
            BigDecimal roundImprovementRate = calculateRoundImprovement(roundScores);

            SmartClassRankingVO ranking = SmartClassRankingVO.builder()
                    .classId(classId)
                    .className(first.getClassName())
                    .gradeId(first.getGradeId())
                    .gradeName(first.getGradeName())
                    .orgUnitId(first.getOrgUnitId())
                    .orgUnitName(first.getOrgUnitName())
                    .teacherId(first.getTeacherId())
                    .teacherName(first.getTeacherName())
                    .studentCount(first.getClassSize())
                    .checkCount(checkCount)
                    .totalRounds(totalRounds)
                    .totalScore(totalScore)
                    .weightedScore(weightedScore)
                    .avgScorePerCheck(avgScorePerCheck)
                    .avgScorePerRound(avgScorePerRound)
                    .categoryScores(categoryScores)
                    .roundScores(roundScores)
                    .roundImprovementRate(roundImprovementRate)
                    .build();

            rankings.add(ranking);
        }

        // 根据对比模式排序
        Comparator<SmartClassRankingVO> comparator = getComparator(compareMode);
        if ("desc".equalsIgnoreCase(filters.getSortOrderOrDefault())) {
            comparator = comparator.reversed();
        }
        rankings.sort(comparator);

        // 计算全校和年级平均分
        BigDecimal overallAvgScore = calculateOverallAvg(rankings, compareMode);
        List<SmartRankingResultVO.GradeAvgVO> gradeAvgScores = calculateGradeAvg(rankings, compareMode);

        // 设置排名和与平均差距
        for (int i = 0; i < rankings.size(); i++) {
            SmartClassRankingVO vo = rankings.get(i);
            vo.setRanking(i + 1);
            vo.setScoreLevel(calculateScoreLevel(i + 1, rankings.size()));

            BigDecimal scoreForCompare = getScoreByMode(vo, compareMode);
            vo.setVsAvg(scoreForCompare.subtract(overallAvgScore));

            // 年级平均差距
            BigDecimal gradeAvg = gradeAvgScores.stream()
                    .filter(ga -> ga.getGradeId().equals(vo.getGradeId()))
                    .map(SmartRankingResultVO.GradeAvgVO::getAvgScore)
                    .findFirst()
                    .orElse(overallAvgScore);
            vo.setVsGradeAvg(scoreForCompare.subtract(gradeAvg));
        }

        // 设置年级排名
        setGradeRankings(rankings, compareMode);

        // 获取覆盖信息
        CheckPlan plan = checkPlanMapper.selectById(filters.getPlanId());
        CheckCoverageVO coverage = calculateCoverage(plan, records);

        // 可比较集合信息
        SmartRankingResultVO.ComparableSetVO comparableSet = buildComparableSetFromDeductions(rankings, allDeductions);

        // 生成警告和提示
        List<String> warnings = new ArrayList<>();
        List<String> tips = new ArrayList<>();

        if (coverage.getMissedClasses() > 0) {
            warnings.add(String.format("%d个班级因缺检未参与排名", coverage.getMissedClasses()));
        }
        if (!comparableSet.getFullyComparable()) {
            warnings.addAll(comparableSet.getNonComparableReasons());
        }
        if ("average".equals(compareMode)) {
            tips.add("当前按每轮平均分排名，更适合轮次不一致的情况");
        }

        // 分页
        long total = rankings.size();
        int pageNum = filters.getPageNumOrDefault();
        int pageSize = filters.getPageSizeOrDefault();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, rankings.size());

        List<SmartClassRankingVO> pagedRankings = start < rankings.size()
                ? rankings.subList(start, end)
                : Collections.emptyList();

        return SmartRankingResultVO.builder()
                .rankings(pagedRankings)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .coverage(coverage)
                .comparableSet(comparableSet)
                .overallAvgScore(overallAvgScore)
                .gradeAvgScores(gradeAvgScores)
                .compareMode(compareMode)
                .sortBy(filters.getSortByOrDefault())
                .sortOrder(filters.getSortOrderOrDefault())
                .warnings(warnings)
                .tips(tips)
                .build();
    }

    @Override
    public DynamicCategoryStatsVO getDynamicCategoryStats(SmartStatisticsFilters filters) {
        List<CheckRecordNew> records = getFilteredRecords(filters);
        if (records.isEmpty()) {
            return DynamicCategoryStatsVO.builder()
                    .detectedCategories(Collections.emptyList())
                    .categoryStats(Collections.emptyList())
                    .totalScore(BigDecimal.ZERO)
                    .build();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 获取扣分明细（新版没有独立的类别统计表，从扣分明细聚合）
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .in(CheckRecordDeductionNew::getRecordId, recordIds)
        );

        // 按类别聚合
        Map<Long, List<CheckRecordDeductionNew>> groupedByCategory = allDeductions.stream()
                .filter(d -> d.getCategoryId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getCategoryId));

        // 总扣分
        BigDecimal grandTotal = allDeductions.stream()
                .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 构建识别到的类别
        List<DynamicCategoryStatsVO.DetectedCategoryVO> detectedCategories = new ArrayList<>();
        List<DynamicCategoryStatsVO.CategoryStatsDetailVO> categoryStats = new ArrayList<>();

        for (Map.Entry<Long, List<CheckRecordDeductionNew>> entry : groupedByCategory.entrySet()) {
            Long categoryId = entry.getKey();
            List<CheckRecordDeductionNew> deductions = entry.getValue();
            CheckRecordDeductionNew first = deductions.get(0);

            // 识别到的类别
            Set<Long> recordIdsForCategory = deductions.stream()
                    .map(CheckRecordDeductionNew::getRecordId)
                    .collect(Collectors.toSet());

            detectedCategories.add(DynamicCategoryStatsVO.DetectedCategoryVO.builder()
                    .categoryId(categoryId)
                    .categoryCode(null)  // 新版扣分明细中没有categoryCode字段
                    .categoryName(first.getCategoryName())
                    .categoryType(null)  // 新版扣分明细中没有categoryType字段
                    .checkCount(recordIdsForCategory.size())
                    .build());

            // 详细统计
            BigDecimal totalScore = deductions.stream()
                    .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? totalScore.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Set<Long> affectedClasses = deductions.stream()
                    .map(CheckRecordDeductionNew::getClassId)
                    .collect(Collectors.toSet());

            int personCount = deductions.stream()
                    .mapToInt(d -> d.getPersonCount() != null ? d.getPersonCount() : 0)
                    .sum();

            // 高频扣分项
            List<DynamicCategoryStatsVO.TopItemVO> topItems = buildTopItemsFromDeductions(deductions);

            // 轮次分布
            List<DynamicCategoryStatsVO.RoundDistVO> roundDistribution = buildRoundDistributionFromDeductions(deductions);

            categoryStats.add(DynamicCategoryStatsVO.CategoryStatsDetailVO.builder()
                    .categoryId(categoryId)
                    .categoryCode(null)
                    .categoryName(first.getCategoryName())
                    .categoryType(null)
                    .totalScore(totalScore)
                    .percentage(percentage)
                    .triggerCount(deductions.size())
                    .affectedClasses(affectedClasses.size())
                    .personCount(personCount)
                    .topItems(topItems)
                    .roundDistribution(roundDistribution)
                    .build());
        }

        // 按扣分排序
        categoryStats.sort(Comparator.comparing(DynamicCategoryStatsVO.CategoryStatsDetailVO::getTotalScore).reversed());

        // 问题最多的类别
        String topCategory = categoryStats.isEmpty() ? null : categoryStats.get(0).getCategoryName();
        BigDecimal topCategoryPercentage = categoryStats.isEmpty() ? BigDecimal.ZERO : categoryStats.get(0).getPercentage();

        return DynamicCategoryStatsVO.builder()
                .detectedCategories(detectedCategories)
                .categoryStats(categoryStats)
                .totalScore(grandTotal)
                .topCategory(topCategory)
                .topCategoryPercentage(topCategoryPercentage)
                .build();
    }

    @Override
    public RoundAnalysisVO getRoundAnalysis(SmartStatisticsFilters filters) {
        List<CheckRecordNew> records = getFilteredRecords(filters);
        if (records.isEmpty()) {
            return RoundAnalysisVO.builder()
                    .maxRounds(0)
                    .avgRounds(BigDecimal.ZERO)
                    .roundDistribution(Collections.emptyList())
                    .roundComparison(Collections.emptyList())
                    .overallImprovementRate(BigDecimal.ZERO)
                    .improvedClasses(0)
                    .worsenedClasses(0)
                    .stableClasses(0)
                    .insights(Collections.emptyList())
                    .build();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 获取所有扣分明细
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .in(CheckRecordDeductionNew::getRecordId, recordIds)
        );

        // 按班级和轮次分组
        Map<Long, Map<Integer, List<CheckRecordDeductionNew>>> deductionsByClassAndRound = allDeductions.stream()
                .collect(Collectors.groupingBy(
                        CheckRecordDeductionNew::getClassId,
                        Collectors.groupingBy(item -> item.getCheckRound() != null ? item.getCheckRound() : 1)
                ));

        // 计算最大轮次
        int maxRounds = allDeductions.stream()
                .mapToInt(item -> item.getCheckRound() != null ? item.getCheckRound() : 1)
                .max()
                .orElse(1);

        // 轮次分布统计
        Map<Integer, Long> roundCountMap = allDeductions.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getCheckRound() != null ? item.getCheckRound() : 1,
                        Collectors.counting()
                ));

        List<RoundAnalysisVO.RoundDistributionVO> roundDistribution = new ArrayList<>();
        long totalItems = allDeductions.size();

        for (int round = 1; round <= maxRounds; round++) {
            long count = roundCountMap.getOrDefault(round, 0L);
            int finalRound = round;
            Set<Long> classIds = allDeductions.stream()
                    .filter(item -> (item.getCheckRound() != null ? item.getCheckRound() : 1) == finalRound)
                    .map(CheckRecordDeductionNew::getClassId)
                    .collect(Collectors.toSet());

            roundDistribution.add(RoundAnalysisVO.RoundDistributionVO.builder()
                    .roundCount(round)
                    .recordCount((int) count)
                    .classCount(classIds.size())
                    .percentage(totalItems > 0
                            ? BigDecimal.valueOf(count * 100).divide(BigDecimal.valueOf(totalItems), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO)
                    .build());
        }

        // 轮次对比
        List<RoundAnalysisVO.RoundComparisonVO> roundComparison = new ArrayList<>();
        Map<Long, BigDecimal> prevRoundScoreByClass = new HashMap<>();
        int improvedClasses = 0;
        int worsenedClasses = 0;
        int stableClasses = 0;

        for (int round = 1; round <= maxRounds; round++) {
            int finalRound = round;

            // 该轮的所有班级扣分
            Map<Long, BigDecimal> currentRoundScoreByClass = new HashMap<>();

            for (Map.Entry<Long, Map<Integer, List<CheckRecordDeductionNew>>> classEntry : deductionsByClassAndRound.entrySet()) {
                Long classId = classEntry.getKey();
                List<CheckRecordDeductionNew> roundDeductions = classEntry.getValue().getOrDefault(finalRound, Collections.emptyList());

                BigDecimal roundScore = roundDeductions.stream()
                        .map(item -> item.getActualScore() != null ? item.getActualScore() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                currentRoundScoreByClass.put(classId, roundScore);
            }

            // 统计
            BigDecimal totalScore = currentRoundScoreByClass.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgScore = currentRoundScoreByClass.size() > 0
                    ? totalScore.divide(BigDecimal.valueOf(currentRoundScoreByClass.size()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 与上轮对比
            int improved = 0;
            int worsened = 0;
            int stable = 0;
            BigDecimal vsPreRound = BigDecimal.ZERO;

            if (round > 1 && !prevRoundScoreByClass.isEmpty()) {
                BigDecimal prevAvg = prevRoundScoreByClass.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(prevRoundScoreByClass.size()), 2, RoundingMode.HALF_UP);

                vsPreRound = avgScore.subtract(prevAvg);

                for (Map.Entry<Long, BigDecimal> entry : currentRoundScoreByClass.entrySet()) {
                    BigDecimal prev = prevRoundScoreByClass.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                    int cmp = entry.getValue().compareTo(prev);
                    if (cmp < 0) improved++;
                    else if (cmp > 0) worsened++;
                    else stable++;
                }
            }

            roundComparison.add(RoundAnalysisVO.RoundComparisonVO.builder()
                    .round(round)
                    .classCount(currentRoundScoreByClass.size())
                    .avgScore(avgScore)
                    .totalScore(totalScore)
                    .vsPreRound(vsPreRound)
                    .improvedCount(improved)
                    .worsenedCount(worsened)
                    .stableCount(stable)
                    .improvementRate(currentRoundScoreByClass.size() > 0
                            ? BigDecimal.valueOf(improved * 100).divide(BigDecimal.valueOf(currentRoundScoreByClass.size()), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO)
                    .build());

            // 最后一轮统计
            if (round == maxRounds) {
                improvedClasses = improved;
                worsenedClasses = worsened;
                stableClasses = stable;
            }

            prevRoundScoreByClass = currentRoundScoreByClass;
        }

        // 整体改善率
        BigDecimal overallImprovementRate = BigDecimal.ZERO;
        if (roundComparison.size() >= 2) {
            BigDecimal firstAvg = roundComparison.get(0).getAvgScore();
            BigDecimal lastAvg = roundComparison.get(roundComparison.size() - 1).getAvgScore();
            if (firstAvg.compareTo(BigDecimal.ZERO) > 0) {
                overallImprovementRate = firstAvg.subtract(lastAvg)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(firstAvg, 2, RoundingMode.HALF_UP);
            }
        }

        // 生成洞察
        List<String> insights = new ArrayList<>();
        if (maxRounds > 1) {
            if (overallImprovementRate.compareTo(BigDecimal.ZERO) > 0) {
                insights.add(String.format("多轮检查后整体改善%.1f%%", overallImprovementRate.doubleValue()));
            } else if (overallImprovementRate.compareTo(BigDecimal.ZERO) < 0) {
                insights.add(String.format("多轮检查后整体恶化%.1f%%", overallImprovementRate.abs().doubleValue()));
            }
            insights.add(String.format("%d个班级表现改善，%d个班级表现变差", improvedClasses, worsenedClasses));
        } else {
            insights.add("本计划检查均为单轮检查");
        }

        // 平均轮次
        BigDecimal avgRounds = records.size() > 0
                ? BigDecimal.valueOf(allDeductions.stream()
                        .mapToInt(item -> item.getCheckRound() != null ? item.getCheckRound() : 1)
                        .sum())
                        .divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RoundAnalysisVO.builder()
                .maxRounds(maxRounds)
                .avgRounds(avgRounds)
                .roundDistribution(roundDistribution)
                .roundComparison(roundComparison)
                .overallImprovementRate(overallImprovementRate)
                .improvedClasses(improvedClasses)
                .worsenedClasses(worsenedClasses)
                .stableClasses(stableClasses)
                .insights(insights)
                .build();
    }

    @Override
    public ClassTrackingVO getClassTracking(Long planId, Long classId) {
        // 简化实现，后续可扩展
        SmartStatisticsFilters filters = SmartStatisticsFilters.builder()
                .planId(planId)
                .classIds(Arrays.asList(classId))
                .build();

        SmartRankingResultVO rankingResult = getSmartRanking(filters);

        if (rankingResult.getRankings().isEmpty()) {
            return null;
        }

        SmartClassRankingVO ranking = rankingResult.getRankings().get(0);

        return ClassTrackingVO.builder()
                .classId(classId)
                .className(ranking.getClassName())
                .gradeName(ranking.getGradeName())
                .teacherName(ranking.getTeacherName())
                .studentCount(ranking.getStudentCount())
                .totalChecks(ranking.getCheckCount())
                .totalRounds(ranking.getTotalRounds())
                .totalScore(ranking.getTotalScore())
                .avgScorePerCheck(ranking.getAvgScorePerCheck())
                .avgScorePerRound(ranking.getAvgScorePerRound())
                .ranking(ranking.getRanking())
                .gradeRanking(ranking.getGradeRanking())
                .scoreLevel(ranking.getScoreLevel())
                .vsOverallAvg(ranking.getVsAvg())
                .vsGradeAvg(ranking.getVsGradeAvg())
                .build();
    }

    @Override
    public CheckCoverageVO getCheckCoverage(Long planId, SmartStatisticsFilters filters) {
        CheckPlan plan = checkPlanMapper.selectById(planId);
        if (plan == null) {
            return null;
        }

        if (filters == null) {
            filters = SmartStatisticsFilters.builder().planId(planId).build();
        }

        List<CheckRecordNew> records = getFilteredRecords(filters);
        return calculateCoverage(plan, records);
    }

    @Override
    public List<String> generateInsights(SmartStatisticsFilters filters) {
        List<String> insights = new ArrayList<>();

        SmartStatisticsOverviewVO overview = getSmartOverview(filters);
        if (overview == null) {
            return insights;
        }

        // 覆盖率洞察
        if (overview.getCoverage() != null && overview.getCoverage().getCoverageRate() != null) {
            BigDecimal rate = overview.getCoverage().getCoverageRate();
            if (rate.compareTo(BigDecimal.valueOf(100)) >= 0) {
                insights.add("检查覆盖率100%，所有目标班级均已检查");
            } else if (rate.compareTo(BigDecimal.valueOf(80)) >= 0) {
                insights.add(String.format("检查覆盖率%.0f%%，基本覆盖所有目标班级", rate.doubleValue()));
            } else {
                insights.add(String.format("检查覆盖率仅%.0f%%，建议增加检查频次", rate.doubleValue()));
            }
        }

        // 趋势洞察
        if (overview.getTrend() != null) {
            String direction = overview.getTrend().getDirection();
            BigDecimal percentage = overview.getTrend().getPercentage();
            if ("down".equals(direction) && percentage != null) {
                insights.add(String.format("扣分呈下降趋势，较上期改善%.1f%%", percentage.abs().doubleValue()));
            } else if ("up".equals(direction) && percentage != null) {
                insights.add(String.format("扣分呈上升趋势，较上期增加%.1f%%，需要关注", percentage.doubleValue()));
            }
        }

        // 扣分分布洞察
        DynamicCategoryStatsVO categoryStats = getDynamicCategoryStats(filters);
        if (categoryStats != null && categoryStats.getTopCategory() != null) {
            insights.add(String.format("%s类问题占比最高(%.0f%%)，建议重点关注",
                    categoryStats.getTopCategory(),
                    categoryStats.getTopCategoryPercentage().doubleValue()));
        }

        return insights;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 内部洞察生成方法，避免循环调用
     */
    private List<String> generateInsightsInternal(CheckCoverageVO coverage,
                                                   SmartStatisticsOverviewVO.TrendInfo trend,
                                                   SmartStatisticsFilters filters) {
        List<String> insights = new ArrayList<>();

        // 覆盖率洞察
        if (coverage != null && coverage.getCoverageRate() != null) {
            BigDecimal rate = coverage.getCoverageRate();
            if (rate.compareTo(BigDecimal.valueOf(100)) >= 0) {
                insights.add("检查覆盖率100%，所有目标班级均已检查");
            } else if (rate.compareTo(BigDecimal.valueOf(80)) >= 0) {
                insights.add(String.format("检查覆盖率%.0f%%，基本覆盖所有目标班级", rate.doubleValue()));
            } else {
                insights.add(String.format("检查覆盖率仅%.0f%%，建议增加检查频次", rate.doubleValue()));
            }
        }

        // 趋势洞察
        if (trend != null) {
            String direction = trend.getDirection();
            BigDecimal percentage = trend.getPercentage();
            if ("down".equals(direction) && percentage != null) {
                insights.add(String.format("扣分呈下降趋势，较上期改善%.1f%%", percentage.abs().doubleValue()));
            } else if ("up".equals(direction) && percentage != null) {
                insights.add(String.format("扣分呈上升趋势，较上期增加%.1f%%，需要关注", percentage.doubleValue()));
            }
        }

        // 扣分分布洞察
        try {
            DynamicCategoryStatsVO categoryStats = getDynamicCategoryStats(filters);
            if (categoryStats != null && categoryStats.getTopCategory() != null) {
                insights.add(String.format("%s类问题占比最高(%.0f%%)，建议重点关注",
                        categoryStats.getTopCategory(),
                        categoryStats.getTopCategoryPercentage().doubleValue()));
            }
        } catch (Exception e) {
            log.warn("Failed to get category stats for insights", e);
        }

        return insights;
    }

    private List<CheckRecordNew> getFilteredRecords(SmartStatisticsFilters filters) {
        LambdaQueryWrapper<CheckRecordNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRecordNew::getPlanId, filters.getPlanId());

        if (filters.getRecordIds() != null && !filters.getRecordIds().isEmpty()) {
            wrapper.in(CheckRecordNew::getId, filters.getRecordIds());
        }
        if (filters.getStartDate() != null) {
            wrapper.ge(CheckRecordNew::getCheckDate, filters.getStartDate());
        }
        if (filters.getEndDate() != null) {
            wrapper.le(CheckRecordNew::getCheckDate, filters.getEndDate());
        }

        wrapper.eq(CheckRecordNew::getStatus, 1);
        wrapper.orderByDesc(CheckRecordNew::getCheckDate);

        return checkRecordMapper.selectList(wrapper);
    }

    private CheckCoverageVO calculateCoverage(CheckPlan plan, List<CheckRecordNew> records) {
        // 获取计划目标班级数
        int planTargetClasses = getTargetClassCount(plan);

        if (records.isEmpty()) {
            return CheckCoverageVO.builder()
                    .planTargetClasses(planTargetClasses)
                    .actualCheckedClasses(0)
                    .coverageRate(BigDecimal.ZERO)
                    .fullCoverageRecords(0)
                    .partialCoverageRecords(0)
                    .missedClasses(planTargetClasses)
                    .build();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 获取实际检查的班级
        List<CheckRecordClassStatsNew> allClassStats = classStatsMapper.selectList(
                new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                        .in(CheckRecordClassStatsNew::getRecordId, recordIds)
        );

        Set<Long> checkedClassIds = allClassStats.stream()
                .map(CheckRecordClassStatsNew::getClassId)
                .collect(Collectors.toSet());

        int actualCheckedClasses = checkedClassIds.size();

        BigDecimal coverageRate = planTargetClasses > 0
                ? BigDecimal.valueOf(actualCheckedClasses * 100).divide(BigDecimal.valueOf(planTargetClasses), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 统计全覆盖和部分覆盖的记录数
        Map<Long, Set<Long>> classesByRecord = allClassStats.stream()
                .collect(Collectors.groupingBy(
                        CheckRecordClassStatsNew::getRecordId,
                        Collectors.mapping(CheckRecordClassStatsNew::getClassId, Collectors.toSet())
                ));

        int fullCoverage = 0;
        int partialCoverage = 0;
        for (Set<Long> classes : classesByRecord.values()) {
            if (classes.size() >= planTargetClasses) {
                fullCoverage++;
            } else {
                partialCoverage++;
            }
        }

        // 班级检查次数
        Map<Long, Long> checkCountByClass = allClassStats.stream()
                .collect(Collectors.groupingBy(CheckRecordClassStatsNew::getClassId, Collectors.counting()));

        List<CheckCoverageVO.ClassCheckCountVO> classCheckCounts = checkCountByClass.entrySet().stream()
                .map(entry -> {
                    CheckRecordClassStatsNew sample = allClassStats.stream()
                            .filter(cs -> cs.getClassId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);

                    return CheckCoverageVO.ClassCheckCountVO.builder()
                            .classId(entry.getKey())
                            .className(sample != null ? sample.getClassName() : "未知")
                            .checkCount(entry.getValue().intValue())
                            .build();
                })
                .sorted(Comparator.comparing(CheckCoverageVO.ClassCheckCountVO::getCheckCount).reversed())
                .collect(Collectors.toList());

        return CheckCoverageVO.builder()
                .planTargetClasses(planTargetClasses)
                .actualCheckedClasses(actualCheckedClasses)
                .coverageRate(coverageRate.min(BigDecimal.valueOf(100)))
                .fullCoverageRecords(fullCoverage)
                .partialCoverageRecords(partialCoverage)
                .missedClasses(Math.max(0, planTargetClasses - actualCheckedClasses))
                .classCheckCounts(classCheckCounts)
                .build();
    }

    private int getTargetClassCount(CheckPlan plan) {
        String scopeType = plan.getTargetScopeType();
        if ("all".equals(scopeType)) {
            Long count = classMapper.selectCount(
                    new LambdaQueryWrapper<com.school.management.entity.Class>()
                            .eq(com.school.management.entity.Class::getStatus, 1)
            );
            return count != null ? count.intValue() : 0;
        }

        String configJson = plan.getTargetScopeConfig();
        if (configJson == null || configJson.isEmpty()) {
            return 0;
        }

        try {
            Map<String, Object> config = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
            List<Integer> classIds = (List<Integer>) config.get("classIds");
            if (classIds != null && !classIds.isEmpty()) {
                return classIds.size();
            }

            // 如果是按组织单元或年级，需要查询班级数
            List<Integer> orgUnitIds = (List<Integer>) config.get("orgUnitIds");
            List<Integer> gradeIds = (List<Integer>) config.get("gradeIds");

            LambdaQueryWrapper<com.school.management.entity.Class> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(com.school.management.entity.Class::getStatus, 1);

            if (orgUnitIds != null && !orgUnitIds.isEmpty()) {
                wrapper.in(com.school.management.entity.Class::getOrgUnitId, orgUnitIds);
            }
            if (gradeIds != null && !gradeIds.isEmpty()) {
                wrapper.in(com.school.management.entity.Class::getGradeId, gradeIds);
            }

            Long count = classMapper.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("Failed to parse target scope config", e);
            return 0;
        }
    }

    private SmartStatisticsOverviewVO.TrendInfo calculateTrend(List<CheckRecordNew> records, SmartStatisticsFilters filters) {
        if (records.size() < 2) {
            return SmartStatisticsOverviewVO.TrendInfo.builder()
                    .direction("stable")
                    .percentage(BigDecimal.ZERO)
                    .changeValue(BigDecimal.ZERO)
                    .compareBase("数据不足")
                    .build();
        }

        // 按日期排序
        records.sort(Comparator.comparing(CheckRecordNew::getCheckDate));

        int mid = records.size() / 2;
        BigDecimal firstHalfAvg = records.subList(0, mid).stream()
                .map(r -> r.getTotalDeductionScore() != null ? r.getTotalDeductionScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(mid), 2, RoundingMode.HALF_UP);

        BigDecimal secondHalfAvg = records.subList(mid, records.size()).stream()
                .map(r -> r.getTotalDeductionScore() != null ? r.getTotalDeductionScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(records.size() - mid), 2, RoundingMode.HALF_UP);

        BigDecimal changeValue = secondHalfAvg.subtract(firstHalfAvg);
        BigDecimal percentage = firstHalfAvg.compareTo(BigDecimal.ZERO) > 0
                ? changeValue.multiply(BigDecimal.valueOf(100)).divide(firstHalfAvg, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String direction;
        if (percentage.compareTo(BigDecimal.valueOf(5)) > 0) {
            direction = "up";
        } else if (percentage.compareTo(BigDecimal.valueOf(-5)) < 0) {
            direction = "down";
        } else {
            direction = "stable";
        }

        return SmartStatisticsOverviewVO.TrendInfo.builder()
                .direction(direction)
                .percentage(percentage)
                .changeValue(changeValue)
                .compareBase("较前期")
                .build();
    }

    private List<String> generateWarnings(CheckCoverageVO coverage, List<CheckRecordNew> records, List<CheckRecordClassStatsNew> classStats) {
        List<String> warnings = new ArrayList<>();

        if (coverage.getCoverageRate().compareTo(BigDecimal.valueOf(80)) < 0) {
            warnings.add(String.format("检查覆盖率仅%.0f%%，%d个班级未被检查",
                    coverage.getCoverageRate().doubleValue(),
                    coverage.getMissedClasses()));
        }

        if (coverage.getPartialCoverageRecords() > coverage.getFullCoverageRecords()) {
            warnings.add("大部分检查记录未能覆盖所有目标班级");
        }

        return warnings;
    }

    /**
     * 从扣分明细构建类别得分（新版）
     */
    private List<SmartClassRankingVO.CategoryScoreDetailVO> buildCategoryScoresFromDeductions(
            List<CheckRecordDeductionNew> deductions, BigDecimal totalScore) {

        if (deductions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<CheckRecordDeductionNew>> grouped = deductions.stream()
                .filter(d -> d.getCategoryId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getCategoryId));

        BigDecimal totalForPercentage = totalScore.compareTo(BigDecimal.ZERO) > 0 ? totalScore : BigDecimal.ONE;

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<CheckRecordDeductionNew> categoryDeductions = entry.getValue();
                    CheckRecordDeductionNew first = categoryDeductions.get(0);

                    BigDecimal score = categoryDeductions.stream()
                            .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return SmartClassRankingVO.CategoryScoreDetailVO.builder()
                            .categoryId(entry.getKey())
                            .categoryName(first.getCategoryName())
                            .categoryCode(null)
                            .score(score)
                            .percentage(score.multiply(BigDecimal.valueOf(100)).divide(totalForPercentage, 2, RoundingMode.HALF_UP))
                            .itemCount(categoryDeductions.size())
                            .build();
                })
                .sorted(Comparator.comparing(SmartClassRankingVO.CategoryScoreDetailVO::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 从扣分明细构建轮次得分（新版）
     */
    private List<SmartClassRankingVO.RoundScoreVO> buildRoundScoresFromDeductions(List<CheckRecordDeductionNew> deductions) {
        if (deductions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, List<CheckRecordDeductionNew>> deductionsByRound = deductions.stream()
                .collect(Collectors.groupingBy(d -> d.getCheckRound() != null ? d.getCheckRound() : 1));

        List<SmartClassRankingVO.RoundScoreVO> roundScores = new ArrayList<>();
        BigDecimal prevScore = null;

        for (int round = 1; round <= deductionsByRound.size(); round++) {
            List<CheckRecordDeductionNew> roundDeductions = deductionsByRound.get(round);
            if (roundDeductions == null) continue;

            BigDecimal score = roundDeductions.stream()
                    .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal vsPreRound = prevScore != null ? score.subtract(prevScore) : BigDecimal.ZERO;
            String improvement;
            if (prevScore == null) {
                improvement = "first";
            } else if (vsPreRound.compareTo(BigDecimal.ZERO) < 0) {
                improvement = "improved";
            } else if (vsPreRound.compareTo(BigDecimal.ZERO) > 0) {
                improvement = "worsened";
            } else {
                improvement = "stable";
            }

            roundScores.add(SmartClassRankingVO.RoundScoreVO.builder()
                    .round(round)
                    .score(score)
                    .vsPreRound(vsPreRound)
                    .improvement(improvement)
                    .build());

            prevScore = score;
        }

        return roundScores;
    }

    private BigDecimal calculateRoundImprovement(List<SmartClassRankingVO.RoundScoreVO> roundScores) {
        if (roundScores.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal first = roundScores.get(0).getScore();
        BigDecimal last = roundScores.get(roundScores.size() - 1).getScore();

        if (first.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return first.subtract(last).multiply(BigDecimal.valueOf(100)).divide(first, 2, RoundingMode.HALF_UP);
    }

    private Comparator<SmartClassRankingVO> getComparator(String compareMode) {
        switch (compareMode) {
            case "total":
                return Comparator.comparing(SmartClassRankingVO::getTotalScore);
            case "weighted":
                return Comparator.comparing(r -> r.getWeightedScore() != null ? r.getWeightedScore() : r.getTotalScore());
            case "normalized":
                return Comparator.comparing(r -> r.getNormalizedScore() != null ? r.getNormalizedScore() : r.getAvgScorePerRound());
            case "average":
            default:
                return Comparator.comparing(SmartClassRankingVO::getAvgScorePerRound);
        }
    }

    private BigDecimal getScoreByMode(SmartClassRankingVO vo, String mode) {
        switch (mode) {
            case "total":
                return vo.getTotalScore();
            case "weighted":
                return vo.getWeightedScore() != null ? vo.getWeightedScore() : vo.getTotalScore();
            case "normalized":
                return vo.getNormalizedScore() != null ? vo.getNormalizedScore() : vo.getAvgScorePerRound();
            case "average":
            default:
                return vo.getAvgScorePerRound();
        }
    }

    private BigDecimal calculateOverallAvg(List<SmartClassRankingVO> rankings, String mode) {
        if (rankings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = rankings.stream()
                .map(r -> getScoreByMode(r, mode))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(rankings.size()), 2, RoundingMode.HALF_UP);
    }

    private List<SmartRankingResultVO.GradeAvgVO> calculateGradeAvg(List<SmartClassRankingVO> rankings, String mode) {
        Map<Long, List<SmartClassRankingVO>> groupedByGrade = rankings.stream()
                .filter(r -> r.getGradeId() != null)
                .collect(Collectors.groupingBy(SmartClassRankingVO::getGradeId));

        return groupedByGrade.entrySet().stream()
                .map(entry -> {
                    List<SmartClassRankingVO> gradeRankings = entry.getValue();
                    BigDecimal sum = gradeRankings.stream()
                            .map(r -> getScoreByMode(r, mode))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = sum.divide(BigDecimal.valueOf(gradeRankings.size()), 2, RoundingMode.HALF_UP);

                    return SmartRankingResultVO.GradeAvgVO.builder()
                            .gradeId(entry.getKey())
                            .gradeName(gradeRankings.get(0).getGradeName())
                            .avgScore(avg)
                            .classCount(gradeRankings.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void setGradeRankings(List<SmartClassRankingVO> rankings, String mode) {
        Map<Long, List<SmartClassRankingVO>> groupedByGrade = rankings.stream()
                .filter(r -> r.getGradeId() != null)
                .collect(Collectors.groupingBy(SmartClassRankingVO::getGradeId));

        for (List<SmartClassRankingVO> gradeRankings : groupedByGrade.values()) {
            gradeRankings.sort(getComparator(mode));
            for (int i = 0; i < gradeRankings.size(); i++) {
                gradeRankings.get(i).setGradeRanking(i + 1);
            }
        }
    }

    /**
     * 从扣分明细构建可比较集合信息（新版）
     */
    private SmartRankingResultVO.ComparableSetVO buildComparableSetFromDeductions(
            List<SmartClassRankingVO> rankings,
            List<CheckRecordDeductionNew> allDeductions) {

        Set<Long> allCategories = allDeductions.stream()
                .filter(d -> d.getCategoryId() != null)
                .map(CheckRecordDeductionNew::getCategoryId)
                .collect(Collectors.toSet());

        int maxRounds = allDeductions.stream()
                .mapToInt(d -> d.getCheckRound() != null ? d.getCheckRound() : 1)
                .max()
                .orElse(1);

        int minRounds = allDeductions.stream()
                .mapToInt(d -> d.getCheckRound() != null ? d.getCheckRound() : 1)
                .min()
                .orElse(1);

        List<String> nonComparableReasons = new ArrayList<>();
        boolean fullyComparable = true;

        if (maxRounds != minRounds) {
            nonComparableReasons.add(String.format("轮次不一致：最少%d轮，最多%d轮", minRounds, maxRounds));
            fullyComparable = false;
        }

        return SmartRankingResultVO.ComparableSetVO.builder()
                .commonClassCount(rankings.size())
                .commonCategoryCount(allCategories.size())
                .commonMinRounds(minRounds)
                .fullyComparable(fullyComparable)
                .nonComparableReasons(nonComparableReasons)
                .build();
    }

    /**
     * 从扣分明细构建高频扣分项（新版）
     */
    private List<DynamicCategoryStatsVO.TopItemVO> buildTopItemsFromDeductions(List<CheckRecordDeductionNew> deductions) {
        Map<Long, List<CheckRecordDeductionNew>> groupedByItem = deductions.stream()
                .filter(d -> d.getDeductionItemId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getDeductionItemId));

        return groupedByItem.entrySet().stream()
                .map(entry -> {
                    List<CheckRecordDeductionNew> itemList = entry.getValue();
                    CheckRecordDeductionNew first = itemList.get(0);

                    BigDecimal totalScore = itemList.stream()
                            .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Set<Long> classIds = itemList.stream()
                            .map(CheckRecordDeductionNew::getClassId)
                            .collect(Collectors.toSet());

                    return DynamicCategoryStatsVO.TopItemVO.builder()
                            .itemId(entry.getKey())
                            .itemName(first.getDeductionItemName())
                            .triggerCount(itemList.size())
                            .totalScore(totalScore)
                            .classCount(classIds.size())
                            .build();
                })
                .sorted(Comparator.comparing(DynamicCategoryStatsVO.TopItemVO::getTotalScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 从扣分明细构建轮次分布（新版）
     */
    private List<DynamicCategoryStatsVO.RoundDistVO> buildRoundDistributionFromDeductions(List<CheckRecordDeductionNew> deductions) {
        Map<Integer, List<CheckRecordDeductionNew>> groupedByRound = deductions.stream()
                .collect(Collectors.groupingBy(d -> d.getCheckRound() != null ? d.getCheckRound() : 1));

        return groupedByRound.entrySet().stream()
                .map(entry -> {
                    BigDecimal score = entry.getValue().stream()
                            .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return DynamicCategoryStatsVO.RoundDistVO.builder()
                            .round(entry.getKey())
                            .score(score)
                            .count(entry.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparing(DynamicCategoryStatsVO.RoundDistVO::getRound))
                .collect(Collectors.toList());
    }

    private String calculateScoreLevel(int ranking, int total) {
        if (total == 0) return "未知";

        double percentile = (double) ranking / total;

        if (percentile <= 0.25) {
            return "优秀";
        } else if (percentile <= 0.50) {
            return "良好";
        } else if (percentile <= 0.75) {
            return "中等";
        } else {
            return "较差";
        }
    }
}
