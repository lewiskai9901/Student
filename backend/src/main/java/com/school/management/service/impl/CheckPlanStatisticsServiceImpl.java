package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.dto.statistics.*;
import com.school.management.entity.CheckPlan;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import com.school.management.entity.record.CheckRecordDeductionNew;
import com.school.management.mapper.CheckPlanMapper;
import com.school.management.mapper.DailyCheckMapper;
import com.school.management.mapper.record.CheckRecordMapper;
import com.school.management.mapper.record.CheckRecordClassStatsMapper;
import com.school.management.mapper.record.CheckRecordDeductionMapper;
import com.school.management.service.CheckPlanStatisticsService;
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
 * 检查计划统计服务实现
 * 使用新版表结构：check_records_new, check_record_class_stats_new, check_record_deductions_new
 */
@Slf4j
@Service
public class CheckPlanStatisticsServiceImpl implements CheckPlanStatisticsService {

    private final CheckRecordMapper checkRecordMapper;
    private final CheckRecordClassStatsMapper classStatsMapper;
    private final CheckRecordDeductionMapper deductionMapper;
    private final CheckPlanMapper checkPlanMapper;
    private final DailyCheckMapper dailyCheckMapper;

    public CheckPlanStatisticsServiceImpl(
            @Qualifier("newCheckRecordMapper") CheckRecordMapper checkRecordMapper,
            @Qualifier("newCheckRecordClassStatsMapper") CheckRecordClassStatsMapper classStatsMapper,
            @Qualifier("newCheckRecordDeductionMapper") CheckRecordDeductionMapper deductionMapper,
            CheckPlanMapper checkPlanMapper,
            DailyCheckMapper dailyCheckMapper) {
        this.checkRecordMapper = checkRecordMapper;
        this.classStatsMapper = classStatsMapper;
        this.deductionMapper = deductionMapper;
        this.checkPlanMapper = checkPlanMapper;
        this.dailyCheckMapper = dailyCheckMapper;
    }

    @Override
    public PlanStatisticsOverviewVO getOverview(StatisticsFilters filters) {
        Long planId = filters.getPlanId();

        // 获取检查计划信息
        CheckPlan plan = checkPlanMapper.selectById(planId);
        if (plan == null) {
            return null;
        }

        // 获取符合条件的检查记录
        List<CheckRecordNew> records = getFilteredRecords(filters);
        if (records.isEmpty()) {
            return PlanStatisticsOverviewVO.builder()
                    .planId(planId)
                    .planName(plan.getPlanName())
                    .totalChecks(0)
                    .totalClasses(0)
                    .totalScore(BigDecimal.ZERO)
                    .avgScore(BigDecimal.ZERO)
                    .maxScore(BigDecimal.ZERO)
                    .minScore(BigDecimal.ZERO)
                    .totalItems(0)
                    .totalPersons(0)
                    .weightEnabled(false)
                    .startDate(plan.getStartDate())
                    .endDate(plan.getEndDate())
                    .status(plan.getStatus())
                    .build();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 统计汇总
        BigDecimal totalScore = records.stream()
                .map(r -> r.getTotalDeductionScore() != null ? r.getTotalDeductionScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);

        BigDecimal maxScore = records.stream()
                .map(r -> r.getTotalDeductionScore() != null ? r.getTotalDeductionScore() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minScore = records.stream()
                .map(r -> r.getTotalDeductionScore() != null ? r.getTotalDeductionScore() : BigDecimal.ZERO)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 统计涉及班级数
        Set<Long> classIds = new HashSet<>();
        int totalItems = 0;
        int totalPersons = 0;

        for (Long recordId : recordIds) {
            List<CheckRecordClassStatsNew> classStats = classStatsMapper.selectList(
                    new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                            .eq(CheckRecordClassStatsNew::getRecordId, recordId)
            );
            classStats.forEach(cs -> classIds.add(cs.getClassId()));

            List<CheckRecordDeductionNew> deductions = deductionMapper.selectList(
                    new LambdaQueryWrapper<CheckRecordDeductionNew>()
                            .eq(CheckRecordDeductionNew::getRecordId, recordId)
            );
            totalItems += deductions.size();
            totalPersons += deductions.stream()
                    .mapToInt(d -> d.getPersonCount() != null ? d.getPersonCount() : 0)
                    .sum();
        }

        // 新版不支持加权
        boolean weightEnabled = false;

        // 获取最近检查日期
        LocalDate lastCheckDate = records.stream()
                .map(CheckRecordNew::getCheckDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        return PlanStatisticsOverviewVO.builder()
                .planId(planId)
                .planName(plan.getPlanName())
                .totalChecks(records.size())
                .totalClasses(classIds.size())
                .totalScore(totalScore)
                .avgScore(avgScore)
                .maxScore(maxScore)
                .minScore(minScore)
                .totalItems(totalItems)
                .totalPersons(totalPersons)
                .weightEnabled(weightEnabled)
                .lastCheckDate(lastCheckDate)
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .status(plan.getStatus())
                .build();
    }

    @Override
    public List<ClassRankingVO> getClassRanking(StatisticsFilters filters) {
        List<CheckRecordNew> records = getFilteredRecords(filters);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 获取所有班级统计
        List<CheckRecordClassStatsNew> allClassStats = classStatsMapper.selectList(
                new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                        .in(CheckRecordClassStatsNew::getRecordId, recordIds)
        );

        // 班级筛选
        if (filters.getClassIds() != null && !filters.getClassIds().isEmpty()) {
            allClassStats = allClassStats.stream()
                    .filter(cs -> filters.getClassIds().contains(cs.getClassId()))
                    .collect(Collectors.toList());
        }

        // 按班级聚合
        Map<Long, List<CheckRecordClassStatsNew>> groupedByClass = allClassStats.stream()
                .collect(Collectors.groupingBy(CheckRecordClassStatsNew::getClassId));

        // 获取扣分明细用于类别统计
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .in(CheckRecordDeductionNew::getRecordId, recordIds)
        );
        Map<Long, List<CheckRecordDeductionNew>> deductionsByClass = allDeductions.stream()
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getClassId));

        List<ClassRankingVO> rankings = new ArrayList<>();

        for (Map.Entry<Long, List<CheckRecordClassStatsNew>> entry : groupedByClass.entrySet()) {
            Long classId = entry.getKey();
            List<CheckRecordClassStatsNew> stats = entry.getValue();
            CheckRecordClassStatsNew first = stats.get(0);

            BigDecimal totalScore = stats.stream()
                    .map(s -> s.getTotalScore() != null ? s.getTotalScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int checkCount = stats.size();
            BigDecimal avgScorePerCheck = checkCount > 0
                    ? totalScore.divide(BigDecimal.valueOf(checkCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 从扣分明细聚合类别扣分
            List<CheckRecordDeductionNew> classDeductions = deductionsByClass.getOrDefault(classId, Collections.emptyList());
            Map<Long, BigDecimal> categoryScoreMap = new HashMap<>();
            Map<Long, String> categoryNameMap = new HashMap<>();

            for (CheckRecordDeductionNew d : classDeductions) {
                if (d.getCategoryId() != null) {
                    categoryScoreMap.merge(d.getCategoryId(),
                            d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO,
                            BigDecimal::add);
                    categoryNameMap.putIfAbsent(d.getCategoryId(), d.getCategoryName());
                }
            }

            List<ClassRankingVO.CategoryScoreVO> categoryScores = new ArrayList<>();
            BigDecimal classTotalForPercentage = totalScore.compareTo(BigDecimal.ZERO) > 0 ? totalScore : BigDecimal.ONE;

            for (Map.Entry<Long, BigDecimal> ce : categoryScoreMap.entrySet()) {
                BigDecimal score = ce.getValue();
                BigDecimal percentage = score.multiply(BigDecimal.valueOf(100))
                        .divide(classTotalForPercentage, 2, RoundingMode.HALF_UP);

                categoryScores.add(ClassRankingVO.CategoryScoreVO.builder()
                        .categoryId(ce.getKey())
                        .categoryName(categoryNameMap.get(ce.getKey()))
                        .score(score)
                        .percentage(percentage)
                        .build());
            }

            rankings.add(ClassRankingVO.builder()
                    .classId(classId)
                    .className(first.getClassName())
                    .gradeId(first.getGradeId())
                    .gradeName(first.getGradeName())
                    .teacherId(first.getTeacherId())
                    .teacherName(first.getTeacherName())
                    .checkCount(checkCount)
                    .totalScore(totalScore)
                    .avgScorePerCheck(avgScorePerCheck)
                    .categoryScores(categoryScores)
                    .build());
        }

        // 计算全校平均分
        BigDecimal schoolAvg = rankings.isEmpty() ? BigDecimal.ZERO :
                rankings.stream()
                        .map(ClassRankingVO::getTotalScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(rankings.size()), 2, RoundingMode.HALF_UP);

        // 排序（扣分少的排前面）
        String sortBy = filters.getSortByOrDefault();
        boolean asc = "asc".equalsIgnoreCase(filters.getSortOrderOrDefault());

        Comparator<ClassRankingVO> comparator;
        if ("avgScore".equals(sortBy)) {
            comparator = Comparator.comparing(ClassRankingVO::getAvgScorePerCheck);
        } else {
            comparator = Comparator.comparing(ClassRankingVO::getTotalScore);
        }
        if (!asc) {
            comparator = comparator.reversed();
        }
        rankings.sort(comparator);

        // 设置排名和等级
        for (int i = 0; i < rankings.size(); i++) {
            ClassRankingVO vo = rankings.get(i);
            vo.setRanking(i + 1);
            vo.setVsAvg(vo.getTotalScore().subtract(schoolAvg));
            vo.setScoreLevel(calculateScoreLevel(vo.getRanking(), rankings.size()));
        }

        // 设置年级排名
        Map<Long, List<ClassRankingVO>> gradeGroups = rankings.stream()
                .filter(r -> r.getGradeId() != null)
                .collect(Collectors.groupingBy(ClassRankingVO::getGradeId));

        for (List<ClassRankingVO> gradeRankings : gradeGroups.values()) {
            gradeRankings.sort(Comparator.comparing(ClassRankingVO::getTotalScore));
            for (int i = 0; i < gradeRankings.size(); i++) {
                gradeRankings.get(i).setGradeRanking(i + 1);
            }
        }

        // 限制返回数量
        Integer topN = filters.getTopNOrDefault();
        if (topN != null && topN > 0 && rankings.size() > topN) {
            return rankings.subList(0, topN);
        }

        return rankings;
    }

    @Override
    public List<CategoryStatisticsVO> getCategoryStatistics(StatisticsFilters filters) {
        List<CheckRecordNew> records = getFilteredRecords(filters);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 获取扣分明细（新版从扣分明细聚合类别统计）
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .in(CheckRecordDeductionNew::getRecordId, recordIds)
        );

        // 类别筛选
        if (filters.getCategoryIds() != null && !filters.getCategoryIds().isEmpty()) {
            allDeductions = allDeductions.stream()
                    .filter(d -> d.getCategoryId() != null && filters.getCategoryIds().contains(d.getCategoryId()))
                    .collect(Collectors.toList());
        }

        // 按类别聚合
        Map<Long, List<CheckRecordDeductionNew>> groupedByCategory = allDeductions.stream()
                .filter(d -> d.getCategoryId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getCategoryId));

        // 计算总扣分用于百分比
        BigDecimal grandTotal = allDeductions.stream()
                .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotalForPercentage = grandTotal.compareTo(BigDecimal.ZERO) > 0 ? grandTotal : BigDecimal.ONE;

        List<CategoryStatisticsVO> result = new ArrayList<>();

        for (Map.Entry<Long, List<CheckRecordDeductionNew>> entry : groupedByCategory.entrySet()) {
            Long categoryId = entry.getKey();
            List<CheckRecordDeductionNew> deductions = entry.getValue();
            CheckRecordDeductionNew first = deductions.get(0);

            BigDecimal totalScore = deductions.stream()
                    .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = totalScore.multiply(BigDecimal.valueOf(100))
                    .divide(grandTotalForPercentage, 2, RoundingMode.HALF_UP);

            int itemCount = deductions.size();

            Set<Long> classIds = deductions.stream()
                    .map(CheckRecordDeductionNew::getClassId)
                    .collect(Collectors.toSet());

            // 计算人次
            int personCount = deductions.stream()
                    .mapToInt(d -> d.getPersonCount() != null ? d.getPersonCount() : 0)
                    .sum();

            // TOP5扣分项
            Map<Long, List<CheckRecordDeductionNew>> deductionsByItemId = deductions.stream()
                    .filter(d -> d.getDeductionItemId() != null)
                    .collect(Collectors.groupingBy(CheckRecordDeductionNew::getDeductionItemId));

            List<CategoryStatisticsVO.ItemBriefVO> topItems = deductionsByItemId.entrySet().stream()
                    .map(ie -> {
                        List<CheckRecordDeductionNew> items = ie.getValue();
                        CheckRecordDeductionNew firstItem = items.get(0);
                        BigDecimal itemTotal = items.stream()
                                .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        return CategoryStatisticsVO.ItemBriefVO.builder()
                                .itemId(ie.getKey())
                                .itemName(firstItem.getDeductionItemName())
                                .triggerCount(items.size())
                                .totalScore(itemTotal)
                                .build();
                    })
                    .sorted(Comparator.comparing(CategoryStatisticsVO.ItemBriefVO::getTotalScore).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            result.add(CategoryStatisticsVO.builder()
                    .categoryId(categoryId)
                    .categoryName(first.getCategoryName())
                    .categoryCode(null)
                    .totalScore(totalScore)
                    .percentage(percentage)
                    .itemCount(itemCount)
                    .classCount(classIds.size())
                    .personCount(personCount)
                    .topItems(topItems)
                    .build());
        }

        // 按总分排序
        result.sort(Comparator.comparing(CategoryStatisticsVO::getTotalScore).reversed());

        return result;
    }

    @Override
    public List<ItemStatisticsVO> getItemStatistics(StatisticsFilters filters) {
        List<CheckRecordNew> records = getFilteredRecords(filters);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recordIds = records.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

        // 获取所有扣分明细
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .in(CheckRecordDeductionNew::getRecordId, recordIds)
        );

        // 扣分项筛选
        if (filters.getItemIds() != null && !filters.getItemIds().isEmpty()) {
            allDeductions = allDeductions.stream()
                    .filter(d -> d.getDeductionItemId() != null && filters.getItemIds().contains(d.getDeductionItemId()))
                    .collect(Collectors.toList());
        }

        // 类别筛选
        if (filters.getCategoryIds() != null && !filters.getCategoryIds().isEmpty()) {
            allDeductions = allDeductions.stream()
                    .filter(d -> d.getCategoryId() != null && filters.getCategoryIds().contains(d.getCategoryId()))
                    .collect(Collectors.toList());
        }

        // 按扣分项聚合
        Map<Long, List<CheckRecordDeductionNew>> groupedByItem = allDeductions.stream()
                .filter(d -> d.getDeductionItemId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getDeductionItemId));

        List<ItemStatisticsVO> result = new ArrayList<>();

        for (Map.Entry<Long, List<CheckRecordDeductionNew>> entry : groupedByItem.entrySet()) {
            Long itemId = entry.getKey();
            List<CheckRecordDeductionNew> deductions = entry.getValue();
            CheckRecordDeductionNew first = deductions.get(0);

            BigDecimal totalScore = deductions.stream()
                    .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int triggerCount = deductions.size();
            BigDecimal avgScore = triggerCount > 0
                    ? totalScore.divide(BigDecimal.valueOf(triggerCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            int personCount = deductions.stream()
                    .mapToInt(d -> d.getPersonCount() != null ? d.getPersonCount() : 0)
                    .sum();

            Set<Long> classIds = deductions.stream()
                    .map(CheckRecordDeductionNew::getClassId)
                    .collect(Collectors.toSet());

            // TOP3班级
            Map<Long, List<CheckRecordDeductionNew>> deductionsByClass = deductions.stream()
                    .collect(Collectors.groupingBy(CheckRecordDeductionNew::getClassId));

            // 从扣分明细本身获取班级名称
            Map<Long, String> classNameMap = deductions.stream()
                    .filter(d -> d.getClassName() != null)
                    .collect(Collectors.toMap(
                            CheckRecordDeductionNew::getClassId,
                            CheckRecordDeductionNew::getClassName,
                            (a, b) -> a
                    ));

            List<ItemStatisticsVO.ClassItemVO> topClasses = deductionsByClass.entrySet().stream()
                    .map(ce -> {
                        Long classId = ce.getKey();
                        List<CheckRecordDeductionNew> classDeductions = ce.getValue();
                        BigDecimal classTotal = classDeductions.stream()
                                .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        return ItemStatisticsVO.ClassItemVO.builder()
                                .classId(classId)
                                .className(classNameMap.getOrDefault(classId, "未知班级"))
                                .triggerCount(classDeductions.size())
                                .totalScore(classTotal)
                                .build();
                    })
                    .sorted(Comparator.comparing(ItemStatisticsVO.ClassItemVO::getTotalScore).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            result.add(ItemStatisticsVO.builder()
                    .itemId(itemId)
                    .itemName(first.getDeductionItemName())
                    .categoryId(first.getCategoryId())
                    .categoryName(first.getCategoryName())
                    .deductMode(first.getDeductMode())
                    .deductModeDesc(ItemStatisticsVO.getDeductModeDescription(first.getDeductMode()))
                    .triggerCount(triggerCount)
                    .personCount(personCount)
                    .totalScore(totalScore)
                    .avgScore(avgScore)
                    .classCount(classIds.size())
                    .topClasses(topClasses)
                    .build());
        }

        // 按总分排序
        result.sort(Comparator.comparing(ItemStatisticsVO::getTotalScore).reversed());

        // 限制返回数量
        Integer topN = filters.getTopNOrDefault();
        if (topN != null && topN > 0 && result.size() > topN) {
            return result.subList(0, topN);
        }

        return result;
    }

    @Override
    public TrendDataVO getTrendData(StatisticsFilters filters) {
        List<CheckRecordNew> records = getFilteredRecords(filters);
        if (records.isEmpty()) {
            return TrendDataVO.builder()
                    .trendPoints(Collections.emptyList())
                    .summary(TrendDataVO.TrendSummaryVO.builder()
                            .totalChecks(0)
                            .avgChecksPerDay(BigDecimal.ZERO)
                            .avgScorePerDay(BigDecimal.ZERO)
                            .trend("stable")
                            .trendPercentage(BigDecimal.ZERO)
                            .build())
                    .build();
        }

        String granularity = filters.getTrendGranularityOrDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        // 按日期分组
        Map<LocalDate, List<CheckRecordNew>> groupedByDate = records.stream()
                .filter(r -> r.getCheckDate() != null)
                .collect(Collectors.groupingBy(CheckRecordNew::getCheckDate));

        List<TrendDataVO.TrendPointVO> trendPoints = new ArrayList<>();

        for (Map.Entry<LocalDate, List<CheckRecordNew>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<CheckRecordNew> dayRecords = entry.getValue();

            BigDecimal totalScore = dayRecords.stream()
                    .map(r -> r.getTotalDeductionScore() != null ? r.getTotalDeductionScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(dayRecords.size()), 2, RoundingMode.HALF_UP);

            // 统计当天的班级数和人次
            List<Long> dayRecordIds = dayRecords.stream().map(CheckRecordNew::getId).collect(Collectors.toList());

            List<CheckRecordClassStatsNew> dayClassStats = classStatsMapper.selectList(
                    new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                            .in(CheckRecordClassStatsNew::getRecordId, dayRecordIds)
            );
            Set<Long> dayClassIds = dayClassStats.stream()
                    .map(CheckRecordClassStatsNew::getClassId)
                    .collect(Collectors.toSet());

            List<CheckRecordDeductionNew> dayDeductions = deductionMapper.selectList(
                    new LambdaQueryWrapper<CheckRecordDeductionNew>()
                            .in(CheckRecordDeductionNew::getRecordId, dayRecordIds)
            );
            int dayPersonCount = dayDeductions.stream()
                    .mapToInt(d -> d.getPersonCount() != null ? d.getPersonCount() : 0)
                    .sum();

            trendPoints.add(TrendDataVO.TrendPointVO.builder()
                    .date(date)
                    .dateLabel(date.format(formatter))
                    .checkCount(dayRecords.size())
                    .totalScore(totalScore)
                    .avgScore(avgScore)
                    .classCount(dayClassIds.size())
                    .personCount(dayPersonCount)
                    .itemCount(dayDeductions.size())
                    .build());
        }

        // 按日期排序
        trendPoints.sort(Comparator.comparing(TrendDataVO.TrendPointVO::getDate));

        // 计算趋势汇总
        int totalChecks = records.size();
        long dayCount = trendPoints.size();
        BigDecimal avgChecksPerDay = dayCount > 0
                ? BigDecimal.valueOf(totalChecks).divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalScoreAll = trendPoints.stream()
                .map(TrendDataVO.TrendPointVO::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgScorePerDay = dayCount > 0
                ? totalScoreAll.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 计算趋势（对比前半段和后半段）
        String trend = "stable";
        BigDecimal trendPercentage = BigDecimal.ZERO;

        if (trendPoints.size() >= 2) {
            int mid = trendPoints.size() / 2;
            BigDecimal firstHalfAvg = trendPoints.subList(0, mid).stream()
                    .map(TrendDataVO.TrendPointVO::getTotalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(mid), 2, RoundingMode.HALF_UP);

            BigDecimal secondHalfAvg = trendPoints.subList(mid, trendPoints.size()).stream()
                    .map(TrendDataVO.TrendPointVO::getTotalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(trendPoints.size() - mid), 2, RoundingMode.HALF_UP);

            if (firstHalfAvg.compareTo(BigDecimal.ZERO) > 0) {
                trendPercentage = secondHalfAvg.subtract(firstHalfAvg)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(firstHalfAvg, 2, RoundingMode.HALF_UP);

                if (trendPercentage.compareTo(BigDecimal.valueOf(5)) > 0) {
                    trend = "up";
                } else if (trendPercentage.compareTo(BigDecimal.valueOf(-5)) < 0) {
                    trend = "down";
                }
            }
        }

        // 找最高和最低
        TrendDataVO.TrendPointVO maxPoint = trendPoints.stream()
                .max(Comparator.comparing(TrendDataVO.TrendPointVO::getTotalScore))
                .orElse(null);

        TrendDataVO.TrendPointVO minPoint = trendPoints.stream()
                .min(Comparator.comparing(TrendDataVO.TrendPointVO::getTotalScore))
                .orElse(null);

        TrendDataVO.TrendSummaryVO summary = TrendDataVO.TrendSummaryVO.builder()
                .totalChecks(totalChecks)
                .avgChecksPerDay(avgChecksPerDay)
                .avgScorePerDay(avgScorePerDay)
                .trend(trend)
                .trendPercentage(trendPercentage)
                .maxDailyScore(maxPoint != null ? maxPoint.getTotalScore() : BigDecimal.ZERO)
                .maxScoreDate(maxPoint != null ? maxPoint.getDate() : null)
                .minDailyScore(minPoint != null ? minPoint.getTotalScore() : BigDecimal.ZERO)
                .minScoreDate(minPoint != null ? minPoint.getDate() : null)
                .build();

        return TrendDataVO.builder()
                .trendPoints(trendPoints)
                .summary(summary)
                .build();
    }

    @Override
    public String exportStatistics(StatisticsFilters filters) {
        log.info("导出统计数据: planId={}", filters.getPlanId());

        // 获取检查计划信息
        CheckPlan plan = checkPlanMapper.selectById(filters.getPlanId());
        if (plan == null) {
            log.warn("检查计划不存在: planId={}", filters.getPlanId());
            return null;
        }

        // 获取班级排行数据
        List<ClassRankingVO> rankings = getClassRanking(filters);
        if (rankings == null || rankings.isEmpty()) {
            log.warn("无统计数据可导出: planId={}", filters.getPlanId());
            return null;
        }

        try {
            // 生成文件名
            String fileName = String.format("统计报表_%s_%s.xlsx",
                    plan.getPlanName().replaceAll("[\\\\/:*?\"<>|]", "_"),
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            // 使用Apache POI生成Excel
            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("班级统计");

            // 创建表头样式
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // 创建表头
            org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
            String[] headers = {"排名", "班级名称", "总分", "平均分", "检查次数", "扣分项数", "等级"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.xssf.usermodel.XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            for (int i = 0; i < rankings.size(); i++) {
                ClassRankingVO item = rankings.get(i);
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(item.getRanking() != null ? item.getRanking() : i + 1);
                row.createCell(1).setCellValue(item.getClassName() != null ? item.getClassName() : "");
                row.createCell(2).setCellValue(item.getTotalScore() != null ? item.getTotalScore().doubleValue() : 0);
                row.createCell(3).setCellValue(item.getAvgScore() != null ? item.getAvgScore().doubleValue() : 0);
                row.createCell(4).setCellValue(item.getCheckCount() != null ? item.getCheckCount() : 0);
                row.createCell(5).setCellValue(item.getCategoryScores() != null ? item.getCategoryScores().size() : 0);
                row.createCell(6).setCellValue(item.getScoreLevel() != null ? item.getScoreLevel() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            String exportDir = System.getProperty("java.io.tmpdir") + "/exports/";
            java.io.File dir = new java.io.File(exportDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filePath = exportDir + fileName;
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            workbook.close();

            log.info("统计报表导出成功: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("导出统计报表失败", e);
            return null;
        }
    }

    /**
     * 获取符合筛选条件的检查记录
     */
    private List<CheckRecordNew> getFilteredRecords(StatisticsFilters filters) {
        LambdaQueryWrapper<CheckRecordNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRecordNew::getPlanId, filters.getPlanId());

        // 日期范围筛选
        if (filters.getStartDate() != null) {
            wrapper.ge(CheckRecordNew::getCheckDate, filters.getStartDate());
        }
        if (filters.getEndDate() != null) {
            wrapper.le(CheckRecordNew::getCheckDate, filters.getEndDate());
        }

        // 只查询已发布的记录
        wrapper.eq(CheckRecordNew::getStatus, 1);
        wrapper.orderByDesc(CheckRecordNew::getCheckDate);

        return checkRecordMapper.selectList(wrapper);
    }

    /**
     * 根据排名计算等级
     */
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
