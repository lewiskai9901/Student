package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.dto.rating.RatingFrequencyQueryDTO;
import com.school.management.dto.rating.RatingFrequencySummaryVO;
import com.school.management.dto.rating.RatingFrequencyVO;
import com.school.management.entity.*;
import com.school.management.mapper.*;
import com.school.management.service.RatingFrequencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评级频次统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingFrequencyServiceImpl implements RatingFrequencyService {

    private final CheckPlanRatingFrequencyMapper frequencyMapper;
    private final CheckPlanRatingResultMapper resultMapper;
    private final CheckPlanRatingRuleMapper ruleMapper;
    private final CheckPlanRatingLevelMapper levelMapper;
    private final CheckPlanMapper checkPlanMapper;
    private final ClassMapper classMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateFrequency(Long checkPlanId, String periodType, LocalDate periodStart, LocalDate periodEnd) {
        log.info("计算评级频次: checkPlanId={}, periodType={}, period={} to {}",
                checkPlanId, periodType, periodStart, periodEnd);

        // 获取检查计划
        CheckPlan plan = checkPlanMapper.selectById(checkPlanId);
        if (plan == null) {
            log.warn("检查计划不存在: {}", checkPlanId);
            return;
        }

        // 获取该计划的所有启用规则
        List<CheckPlanRatingRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<CheckPlanRatingRule>()
                        .eq(CheckPlanRatingRule::getCheckPlanId, checkPlanId)
                        .eq(CheckPlanRatingRule::getEnabled, 1)
                        .eq(CheckPlanRatingRule::getDeleted, 0)
        );

        if (rules.isEmpty()) {
            log.info("没有启用的评级规则");
            return;
        }

        // 生成周期标签
        String periodLabel = generatePeriodLabel(periodType, periodStart, periodEnd);

        for (CheckPlanRatingRule rule : rules) {
            calculateRuleFrequency(rule, periodType, periodStart, periodEnd, periodLabel);
        }

        log.info("评级频次计算完成");
    }

    @Override
    public void calculateCurrentPeriodFrequency(Long checkPlanId, String periodType) {
        LocalDate today = LocalDate.now();
        LocalDate periodStart;
        LocalDate periodEnd;

        switch (periodType) {
            case CheckPlanRatingFrequency.PERIOD_WEEK:
                periodStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                periodEnd = periodStart.plusDays(6);
                break;
            case CheckPlanRatingFrequency.PERIOD_MONTH:
                periodStart = today.withDayOfMonth(1);
                periodEnd = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case CheckPlanRatingFrequency.PERIOD_QUARTER:
                int quarter = (today.getMonthValue() - 1) / 3;
                periodStart = today.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                periodEnd = periodStart.plusMonths(3).minusDays(1);
                break;
            case CheckPlanRatingFrequency.PERIOD_YEAR:
                periodStart = today.withDayOfYear(1);
                periodEnd = today.with(TemporalAdjusters.lastDayOfYear());
                break;
            default:
                log.warn("不支持的周期类型: {}", periodType);
                return;
        }

        calculateFrequency(checkPlanId, periodType, periodStart, periodEnd);
    }

    @Override
    public List<RatingFrequencyVO> queryFrequency(RatingFrequencyQueryDTO query) {
        LambdaQueryWrapper<CheckPlanRatingFrequency> wrapper = new LambdaQueryWrapper<>();

        if (query.getCheckPlanId() != null) {
            wrapper.eq(CheckPlanRatingFrequency::getCheckPlanId, query.getCheckPlanId());
        }
        if (query.getRuleId() != null) {
            wrapper.eq(CheckPlanRatingFrequency::getRuleId, query.getRuleId());
        }
        if (query.getLevelIds() != null && !query.getLevelIds().isEmpty()) {
            wrapper.in(CheckPlanRatingFrequency::getLevelId, query.getLevelIds());
        }
        if (query.getClassIds() != null && !query.getClassIds().isEmpty()) {
            wrapper.in(CheckPlanRatingFrequency::getClassId, query.getClassIds());
        }
        if (query.getGradeIds() != null && !query.getGradeIds().isEmpty()) {
            wrapper.in(CheckPlanRatingFrequency::getGradeId, query.getGradeIds());
        }
        if (query.getPeriodType() != null) {
            wrapper.eq(CheckPlanRatingFrequency::getPeriodType, query.getPeriodType());
        }
        if (query.getPeriodStart() != null) {
            wrapper.ge(CheckPlanRatingFrequency::getPeriodStart, query.getPeriodStart());
        }
        if (query.getPeriodEnd() != null) {
            wrapper.le(CheckPlanRatingFrequency::getPeriodEnd, query.getPeriodEnd());
        }

        // 排序
        if ("frequency".equals(query.getSortBy())) {
            wrapper.orderBy(true, "ASC".equalsIgnoreCase(query.getSortOrder()),
                    CheckPlanRatingFrequency::getFrequency);
        } else if ("className".equals(query.getSortBy())) {
            wrapper.orderBy(true, "ASC".equalsIgnoreCase(query.getSortOrder()),
                    CheckPlanRatingFrequency::getClassName);
        }

        if (query.getLimit() != null && query.getLimit() > 0) {
            wrapper.last("LIMIT " + query.getLimit());
        }

        List<CheckPlanRatingFrequency> list = frequencyMapper.selectList(wrapper);

        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public RatingFrequencySummaryVO getFrequencySummary(Long checkPlanId, Long ruleId,
                                                          String periodType, LocalDate periodStart, LocalDate periodEnd) {
        // 获取检查计划
        CheckPlan plan = checkPlanMapper.selectById(checkPlanId);
        if (plan == null) {
            return null;
        }

        RatingFrequencySummaryVO summary = new RatingFrequencySummaryVO();
        summary.setCheckPlanId(checkPlanId);
        summary.setCheckPlanName(plan.getPlanName());
        summary.setPeriodType(periodType);
        summary.setPeriodStart(periodStart);
        summary.setPeriodEnd(periodEnd);
        summary.setPeriodLabel(generatePeriodLabel(periodType, periodStart, periodEnd));

        // 查询频次数据
        LambdaQueryWrapper<CheckPlanRatingFrequency> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckPlanRatingFrequency::getCheckPlanId, checkPlanId)
                .eq(CheckPlanRatingFrequency::getPeriodType, periodType)
                .eq(CheckPlanRatingFrequency::getPeriodStart, periodStart);

        if (ruleId != null) {
            wrapper.eq(CheckPlanRatingFrequency::getRuleId, ruleId);
            CheckPlanRatingRule rule = ruleMapper.selectById(ruleId);
            if (rule != null) {
                summary.setRuleId(ruleId);
                summary.setRuleName(rule.getRuleName());
            }
        }

        List<CheckPlanRatingFrequency> frequencies = frequencyMapper.selectList(wrapper);

        if (frequencies.isEmpty()) {
            summary.setTotalClasses(0);
            summary.setTotalRatings(0);
            summary.setLevelDistribution(Collections.emptyList());
            summary.setLevelTopClasses(Collections.emptyList());
            return summary;
        }

        // 统计班级数和总评级次数
        Set<Long> classIds = frequencies.stream()
                .map(CheckPlanRatingFrequency::getClassId)
                .collect(Collectors.toSet());
        summary.setTotalClasses(classIds.size());

        int totalRatings = frequencies.stream()
                .mapToInt(f -> f.getTotalRatings() != null ? f.getTotalRatings() : 0)
                .max().orElse(0);
        summary.setTotalRatings(totalRatings);

        // 按等级分组统计
        Map<Long, List<CheckPlanRatingFrequency>> byLevel = frequencies.stream()
                .collect(Collectors.groupingBy(CheckPlanRatingFrequency::getLevelId));

        List<RatingFrequencySummaryVO.LevelDistributionVO> levelDistribution = new ArrayList<>();
        List<RatingFrequencySummaryVO.LevelTopClassesVO> levelTopClasses = new ArrayList<>();

        for (Map.Entry<Long, List<CheckPlanRatingFrequency>> entry : byLevel.entrySet()) {
            List<CheckPlanRatingFrequency> levelFreqs = entry.getValue();
            CheckPlanRatingFrequency first = levelFreqs.get(0);

            // 等级分布
            RatingFrequencySummaryVO.LevelDistributionVO dist = new RatingFrequencySummaryVO.LevelDistributionVO();
            dist.setLevelId(first.getLevelId());
            dist.setLevelName(first.getLevelName());
            dist.setLevelColor(first.getLevelColor());

            int classCount = levelFreqs.size();
            int totalFreq = levelFreqs.stream().mapToInt(CheckPlanRatingFrequency::getFrequency).sum();
            dist.setClassCount(classCount);
            dist.setTotalFrequency(totalFreq);

            if (summary.getTotalClasses() > 0) {
                dist.setPercentage((double) classCount / summary.getTotalClasses() * 100);
            }
            levelDistribution.add(dist);

            // TOP班级
            RatingFrequencySummaryVO.LevelTopClassesVO topClassesVO = new RatingFrequencySummaryVO.LevelTopClassesVO();
            topClassesVO.setLevelId(first.getLevelId());
            topClassesVO.setLevelName(first.getLevelName());
            topClassesVO.setLevelColor(first.getLevelColor());

            List<RatingFrequencySummaryVO.TopClassVO> topClasses = levelFreqs.stream()
                    .sorted((a, b) -> b.getFrequency().compareTo(a.getFrequency()))
                    .limit(5)
                    .map(f -> {
                        RatingFrequencySummaryVO.TopClassVO tc = new RatingFrequencySummaryVO.TopClassVO();
                        tc.setClassId(f.getClassId());
                        tc.setClassName(f.getClassName());
                        tc.setGradeName(f.getGradeName());
                        tc.setFrequency(f.getFrequency());
                        return tc;
                    })
                    .collect(Collectors.toList());

            // 设置排名
            for (int i = 0; i < topClasses.size(); i++) {
                topClasses.get(i).setRanking(i + 1);
            }
            topClassesVO.setTopClasses(topClasses);
            levelTopClasses.add(topClassesVO);
        }

        summary.setLevelDistribution(levelDistribution);
        summary.setLevelTopClasses(levelTopClasses);

        return summary;
    }

    @Override
    public List<RatingFrequencyVO> getClassFrequencyHistory(Long classId, Long ruleId, String periodType, Integer limit) {
        List<CheckPlanRatingFrequency> list = frequencyMapper.getClassFrequencyHistory(classId, ruleId, periodType);

        if (limit != null && limit > 0 && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<RatingFrequencyVO> getTopClassesByLevel(Long ruleId, Long levelId,
                                                          String periodType, LocalDate periodStart, int topN) {
        List<CheckPlanRatingFrequency> list = frequencyMapper.getTopClassesByLevel(
                ruleId, levelId, periodType, periodStart, topN);

        List<RatingFrequencyVO> result = list.stream().map(this::convertToVO).collect(Collectors.toList());

        // 设置排名
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRanking(i + 1);
        }

        return result;
    }

    /**
     * 计算单个规则的频次
     */
    private void calculateRuleFrequency(CheckPlanRatingRule rule, String periodType,
                                         LocalDate periodStart, LocalDate periodEnd, String periodLabel) {
        log.debug("计算规则频次: ruleId={}, ruleName={}", rule.getId(), rule.getRuleName());

        // 获取该规则在周期内的所有评级结果
        LambdaQueryWrapper<CheckPlanRatingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckPlanRatingResult::getRuleId, rule.getId());

        if (CheckPlanRatingRule.RULE_TYPE_DAILY.equals(rule.getRuleType())) {
            wrapper.ge(CheckPlanRatingResult::getCheckDate, periodStart)
                    .le(CheckPlanRatingResult::getCheckDate, periodEnd);
        } else {
            wrapper.ge(CheckPlanRatingResult::getPeriodStart, periodStart)
                    .le(CheckPlanRatingResult::getPeriodEnd, periodEnd);
        }

        List<CheckPlanRatingResult> results = resultMapper.selectList(wrapper);

        if (results.isEmpty()) {
            log.debug("周期内无评级结果");
            return;
        }

        // 获取规则的等级列表
        List<CheckPlanRatingLevel> levels = levelMapper.selectList(
                new LambdaQueryWrapper<CheckPlanRatingLevel>()
                        .eq(CheckPlanRatingLevel::getRuleId, rule.getId())
                        .orderByAsc(CheckPlanRatingLevel::getLevelOrder)
        );

        Map<Long, CheckPlanRatingLevel> levelMap = levels.stream()
                .collect(Collectors.toMap(CheckPlanRatingLevel::getId, l -> l));

        // 按班级和等级分组统计
        Map<Long, Map<Long, List<CheckPlanRatingResult>>> byClassAndLevel = results.stream()
                .collect(Collectors.groupingBy(
                        CheckPlanRatingResult::getClassId,
                        Collectors.groupingBy(CheckPlanRatingResult::getLevelId)
                ));

        // 计算每个班级的总评级次数
        Map<Long, Long> totalRatingsByClass = results.stream()
                .collect(Collectors.groupingBy(CheckPlanRatingResult::getClassId, Collectors.counting()));

        // 保存频次数据
        for (Map.Entry<Long, Map<Long, List<CheckPlanRatingResult>>> classEntry : byClassAndLevel.entrySet()) {
            Long classId = classEntry.getKey();
            Map<Long, List<CheckPlanRatingResult>> levelResults = classEntry.getValue();

            // 获取班级信息（从第一条结果中获取）
            CheckPlanRatingResult sampleResult = levelResults.values().iterator().next().get(0);
            int totalRatings = totalRatingsByClass.get(classId).intValue();

            for (Map.Entry<Long, List<CheckPlanRatingResult>> levelEntry : levelResults.entrySet()) {
                Long levelId = levelEntry.getKey();
                List<CheckPlanRatingResult> levelResultList = levelEntry.getValue();
                int frequency = levelResultList.size();

                CheckPlanRatingLevel level = levelMap.get(levelId);

                // 查找最近一次获得该等级的日期
                LocalDate lastRatingDate = levelResultList.stream()
                        .map(CheckPlanRatingResult::getCheckDate)
                        .filter(Objects::nonNull)
                        .max(LocalDate::compareTo)
                        .orElse(null);

                // 计算频次占比
                BigDecimal frequencyRate = BigDecimal.ZERO;
                if (totalRatings > 0) {
                    frequencyRate = BigDecimal.valueOf(frequency)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalRatings), 2, RoundingMode.HALF_UP);
                }

                // 插入或更新频次记录
                saveOrUpdateFrequency(
                        rule.getCheckPlanId(), classId, sampleResult.getClassName(),
                        sampleResult.getGradeId(), sampleResult.getGradeName(),
                        rule.getId(), rule.getRuleName(),
                        levelId, level != null ? level.getLevelName() : sampleResult.getLevelName(),
                        level != null ? level.getLevelColor() : null,
                        frequency, totalRatings, frequencyRate,
                        periodType, periodStart, periodEnd, periodLabel, lastRatingDate
                );
            }
        }
    }

    /**
     * 保存或更新频次记录
     */
    private void saveOrUpdateFrequency(Long checkPlanId, Long classId, String className,
                                        Long gradeId, String gradeName,
                                        Long ruleId, String ruleName,
                                        Long levelId, String levelName, String levelColor,
                                        int frequency, int totalRatings, BigDecimal frequencyRate,
                                        String periodType, LocalDate periodStart, LocalDate periodEnd,
                                        String periodLabel, LocalDate lastRatingDate) {
        // 查找是否已存在
        LambdaQueryWrapper<CheckPlanRatingFrequency> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckPlanRatingFrequency::getClassId, classId)
                .eq(CheckPlanRatingFrequency::getRuleId, ruleId)
                .eq(CheckPlanRatingFrequency::getLevelId, levelId)
                .eq(CheckPlanRatingFrequency::getPeriodType, periodType)
                .eq(CheckPlanRatingFrequency::getPeriodStart, periodStart);

        CheckPlanRatingFrequency existing = frequencyMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            existing.setFrequency(frequency);
            existing.setTotalRatings(totalRatings);
            existing.setFrequencyRate(frequencyRate);
            existing.setLastRatingDate(lastRatingDate);
            frequencyMapper.updateById(existing);
        } else {
            // 插入
            CheckPlanRatingFrequency freq = new CheckPlanRatingFrequency();
            freq.setCheckPlanId(checkPlanId);
            freq.setClassId(classId);
            freq.setClassName(className);
            freq.setGradeId(gradeId);
            freq.setGradeName(gradeName);
            freq.setRuleId(ruleId);
            freq.setRuleName(ruleName);
            freq.setLevelId(levelId);
            freq.setLevelName(levelName);
            freq.setLevelColor(levelColor);
            freq.setFrequency(frequency);
            freq.setTotalRatings(totalRatings);
            freq.setFrequencyRate(frequencyRate);
            freq.setPeriodType(periodType);
            freq.setPeriodStart(periodStart);
            freq.setPeriodEnd(periodEnd);
            freq.setPeriodLabel(periodLabel);
            freq.setLastRatingDate(lastRatingDate);
            frequencyMapper.insert(freq);
        }
    }

    /**
     * 生成周期标签
     */
    private String generatePeriodLabel(String periodType, LocalDate periodStart, LocalDate periodEnd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年");

        switch (periodType) {
            case CheckPlanRatingFrequency.PERIOD_WEEK:
                WeekFields weekFields = WeekFields.of(Locale.CHINA);
                int weekNumber = periodStart.get(weekFields.weekOfWeekBasedYear());
                return periodStart.format(formatter) + "第" + weekNumber + "周";
            case CheckPlanRatingFrequency.PERIOD_MONTH:
                return periodStart.format(DateTimeFormatter.ofPattern("yyyy年M月"));
            case CheckPlanRatingFrequency.PERIOD_QUARTER:
                int quarter = (periodStart.getMonthValue() - 1) / 3 + 1;
                return periodStart.format(formatter) + "第" + quarter + "季度";
            case CheckPlanRatingFrequency.PERIOD_SEMESTER:
                int month = periodStart.getMonthValue();
                String semester = month >= 2 && month <= 7 ? "春季学期" : "秋季学期";
                return periodStart.format(formatter) + semester;
            case CheckPlanRatingFrequency.PERIOD_YEAR:
                return periodStart.format(formatter);
            default:
                return periodStart.toString() + " 至 " + periodEnd.toString();
        }
    }

    /**
     * 转换为VO
     */
    private RatingFrequencyVO convertToVO(CheckPlanRatingFrequency entity) {
        RatingFrequencyVO vo = new RatingFrequencyVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
