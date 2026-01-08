package com.school.management.service.rating.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.entity.rating.*;
import com.school.management.mapper.rating.*;
import com.school.management.service.rating.DailyClassSummaryService;
import com.school.management.service.rating.RatingCalculationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评级计算引擎实现
 *
 * @author System
 * @since 4.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingCalculationEngineImpl implements RatingCalculationEngine {

    private final RatingConfigMapper ratingConfigMapper;
    private final RatingRankingSourceMapper rankingSourceMapper;
    private final DailyClassSummaryService dailyClassSummaryService;
    private final RatingResultMapper ratingResultMapper;
    private final RatingCalculationDetailMapper calculationDetailMapper;
    private final RatingChangeLogMapper changeLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<RatingResult> calculateRating(RatingConfig ratingConfig, LocalDate periodStart, LocalDate periodEnd) {
        log.info("开始计算评级: ratingConfigId={}, periodStart={}, periodEnd={}",
                ratingConfig.getId(), periodStart, periodEnd);

        // 1. 获取排名数据源
        LambdaQueryWrapper<RatingRankingSource> sourceQuery = new LambdaQueryWrapper<>();
        sourceQuery.eq(RatingRankingSource::getRatingConfigId, ratingConfig.getId())
                .orderByAsc(RatingRankingSource::getSortOrder);
        List<RatingRankingSource> sources = rankingSourceMapper.selectList(sourceQuery);

        if (sources.isEmpty()) {
            log.warn("评级配置没有数据源: ratingConfigId={}", ratingConfig.getId());
            return Collections.emptyList();
        }

        // 2. 获取周期内的每日汇总
        List<DailyClassSummary> summaries = dailyClassSummaryService.getDailySummaries(
                ratingConfig.getCheckPlanId(), periodStart, periodEnd);

        if (summaries.isEmpty()) {
            log.info("周期内无检查数据");
            return Collections.emptyList();
        }

        // 3. 按班级分组并计算得分
        Map<Long, List<DailyClassSummary>> summariesByClass = summaries.stream()
                .collect(Collectors.groupingBy(DailyClassSummary::getClassId));

        List<ClassScore> classScores = new ArrayList<>();
        for (Map.Entry<Long, List<DailyClassSummary>> entry : summariesByClass.entrySet()) {
            Long classId = entry.getKey();
            List<DailyClassSummary> classSummaries = entry.getValue();

            // 计算班级最终得分
            BigDecimal finalScore = calculateFinalScore(sources, classSummaries);

            classScores.add(new ClassScore(classId, finalScore, classSummaries));
        }

        // 4. 排序和排名
        classScores.sort(Comparator.comparing(ClassScore::getFinalScore).reversed());
        assignRanking(classScores);

        // 5. 应用划分规则
        Set<Long> awardedClassIds = applyDivisionRule(ratingConfig, classScores);

        // 6. 创建评级结果
        List<RatingResult> results = new ArrayList<>();
        for (ClassScore classScore : classScores) {
            RatingResult result = createRatingResult(ratingConfig, periodStart, periodEnd,
                    classScore, awardedClassIds.contains(classScore.getClassId()));
            results.add(result);

            // 保存计算明细
            saveCalculationDetails(result, classScore.getSummaries());
        }

        // 7. 保存评级结果
        for (RatingResult result : results) {
            ratingResultMapper.insert(result);

            // 记录变更日志
            logChange(result, "CREATED", "系统自动计算", null, result.getStatus());
        }

        log.info("评级计算完成: ratingConfigId={}, 结果数={}", ratingConfig.getId(), results.size());
        return results;
    }

    @Override
    @Transactional
    public List<RatingResult> recalculateRating(Long ratingConfigId, LocalDate periodStart,
            LocalDate periodEnd, Long appealId) {
        log.info("重新计算评级: ratingConfigId={}, periodStart={}, periodEnd={}, appealId={}",
                ratingConfigId, periodStart, periodEnd, appealId);

        // 1. 删除旧的评级结果
        LambdaQueryWrapper<RatingResult> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(RatingResult::getRatingConfigId, ratingConfigId)
                .eq(RatingResult::getPeriodStart, periodStart)
                .eq(RatingResult::getPeriodEnd, periodEnd);
        List<RatingResult> oldResults = ratingResultMapper.selectList(deleteQuery);
        oldResults.forEach(result -> {
            logChange(result, "RECALCULATED", "申诉成功，重新计算", result.getStatus(), "DRAFT");
        });
        ratingResultMapper.delete(deleteQuery);

        // 2. 重新计算
        RatingConfig ratingConfig = ratingConfigMapper.selectById(ratingConfigId);
        return calculateRating(ratingConfig, periodStart, periodEnd);
    }

    @Override
    public void calculateDailyRatings(Long checkPlanId, LocalDate checkDate) {
        log.info("计算当日评级: checkPlanId={}, checkDate={}", checkPlanId, checkDate);

        // 查找该计划下的所有日评级配置
        LambdaQueryWrapper<RatingConfig> query = new LambdaQueryWrapper<>();
        query.eq(RatingConfig::getCheckPlanId, checkPlanId)
                .eq(RatingConfig::getRatingType, "DAILY")
                .eq(RatingConfig::getEnabled, 1);
        List<RatingConfig> configs = ratingConfigMapper.selectList(query);

        for (RatingConfig config : configs) {
            calculateRating(config, checkDate, checkDate);
        }
    }

    @Override
    public void calculateWeeklyRatings(Long checkPlanId, LocalDate weekStartDate) {
        log.info("计算周评级: checkPlanId={}, weekStartDate={}", checkPlanId, weekStartDate);

        // 计算周结束日期（周起始日+6天）
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        // 查找该计划下的所有周评级配置
        LambdaQueryWrapper<RatingConfig> query = new LambdaQueryWrapper<>();
        query.eq(RatingConfig::getCheckPlanId, checkPlanId)
                .eq(RatingConfig::getRatingType, "WEEKLY")
                .eq(RatingConfig::getEnabled, 1);
        List<RatingConfig> configs = ratingConfigMapper.selectList(query);

        for (RatingConfig config : configs) {
            calculateRating(config, weekStartDate, weekEndDate);
        }
    }

    @Override
    public void calculateMonthlyRatings(Long checkPlanId, int year, int month) {
        log.info("计算月评级: checkPlanId={}, year={}, month={}", checkPlanId, year, month);

        // 计算月的第一天和最后一天
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        // 查找该计划下的所有月评级配置
        LambdaQueryWrapper<RatingConfig> query = new LambdaQueryWrapper<>();
        query.eq(RatingConfig::getCheckPlanId, checkPlanId)
                .eq(RatingConfig::getRatingType, "MONTHLY")
                .eq(RatingConfig::getEnabled, 1);
        List<RatingConfig> configs = ratingConfigMapper.selectList(query);

        for (RatingConfig config : configs) {
            calculateRating(config, monthStart, monthEnd);
        }
    }

    /**
     * 计算班级最终得分
     */
    private BigDecimal calculateFinalScore(List<RatingRankingSource> sources,
            List<DailyClassSummary> summaries) {

        if (sources.size() == 1) {
            // 单一数据源
            RatingRankingSource source = sources.get(0);
            return calculateSourceScore(source, summaries);
        } else {
            // 组合排名（加权求和）
            BigDecimal finalScore = BigDecimal.ZERO;
            for (RatingRankingSource source : sources) {
                BigDecimal sourceScore = calculateSourceScore(source, summaries);
                BigDecimal weightedScore = sourceScore.multiply(source.getWeight());
                finalScore = finalScore.add(weightedScore);
            }
            return finalScore.setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * 计算单个数据源的得分
     */
    private BigDecimal calculateSourceScore(RatingRankingSource source,
            List<DailyClassSummary> summaries) {

        BigDecimal totalScore = BigDecimal.ZERO;
        int validDays = 0;

        for (DailyClassSummary summary : summaries) {
            BigDecimal dayScore = extractScore(source, summary);
            if (dayScore != null) {
                totalScore = totalScore.add(dayScore);
                validDays++;
            } else {
                // 处理缺失数据
                if ("ZERO".equals(source.getMissingDataStrategy())) {
                    validDays++;
                }
                // SKIP策略则不计入
            }
        }

        if (validDays == 0) {
            return "ZERO".equals(source.getMissingDataStrategy()) ? BigDecimal.ZERO : null;
        }

        // 返回平均分
        return totalScore.divide(BigDecimal.valueOf(validDays), 2, RoundingMode.HALF_UP);
    }

    /**
     * 从汇总中提取得分
     */
    private BigDecimal extractScore(RatingRankingSource source, DailyClassSummary summary) {
        try {
            switch (source.getSourceType()) {
                case "TOTAL_SCORE":
                    return source.getUseWeighted() == 1 ?
                            summary.getWeightedTotalScore() : summary.getTotalScore();

                case "CATEGORY":
                    Map<String, Map<String, BigDecimal>> categoryScores = objectMapper.readValue(
                            summary.getCategoryScores(),
                            new TypeReference<Map<String, Map<String, BigDecimal>>>() {});
                    Map<String, BigDecimal> categoryScore = categoryScores.get(source.getSourceId().toString());
                    if (categoryScore != null) {
                        return source.getUseWeighted() == 1 ?
                                categoryScore.get("weighted_score") : categoryScore.get("score");
                    }
                    return null;

                case "DEDUCTION_ITEM":
                    Map<String, Map<String, BigDecimal>> itemScores = objectMapper.readValue(
                            summary.getItemScores(),
                            new TypeReference<Map<String, Map<String, BigDecimal>>>() {});
                    Map<String, BigDecimal> itemScore = itemScores.get(source.getSourceId().toString());
                    if (itemScore != null) {
                        return source.getUseWeighted() == 1 ?
                                itemScore.get("weighted_score") : itemScore.get("score");
                    }
                    return null;

                default:
                    log.warn("未知的数据源类型: {}", source.getSourceType());
                    return null;
            }
        } catch (Exception e) {
            log.error("提取得分失败", e);
            return null;
        }
    }

    /**
     * 分配排名
     */
    private void assignRanking(List<ClassScore> classScores) {
        int currentRanking = 1;
        BigDecimal lastScore = null;

        for (int i = 0; i < classScores.size(); i++) {
            ClassScore score = classScores.get(i);

            if (lastScore == null || score.getFinalScore().compareTo(lastScore) < 0) {
                currentRanking = i + 1;
            }

            score.setRanking(currentRanking);
            lastScore = score.getFinalScore();
        }
    }

    /**
     * 应用划分规则
     */
    private Set<Long> applyDivisionRule(RatingConfig config, List<ClassScore> classScores) {
        Set<Long> awardedClassIds = new HashSet<>();
        int totalClasses = classScores.size();

        switch (config.getDivisionMethod()) {
            case "TOP_N":
                int topN = config.getDivisionValue().intValue();
                for (int i = 0; i < Math.min(topN, totalClasses); i++) {
                    // 处理并列排名
                    ClassScore current = classScores.get(i);
                    if (current.getRanking() <= topN) {
                        awardedClassIds.add(current.getClassId());
                    }
                }
                break;

            case "TOP_PERCENT":
                double topPercent = config.getDivisionValue().doubleValue() / 100;
                int topCount = (int) Math.ceil(totalClasses * topPercent);
                for (int i = 0; i < Math.min(topCount, totalClasses); i++) {
                    awardedClassIds.add(classScores.get(i).getClassId());
                }
                break;

            case "BOTTOM_N":
                int bottomN = config.getDivisionValue().intValue();
                int startIndex = Math.max(0, totalClasses - bottomN);
                for (int i = startIndex; i < totalClasses; i++) {
                    ClassScore current = classScores.get(i);
                    int bottomRanking = totalClasses - bottomN + 1;
                    if (current.getRanking() >= bottomRanking) {
                        awardedClassIds.add(current.getClassId());
                    }
                }
                break;

            case "BOTTOM_PERCENT":
                double bottomPercent = config.getDivisionValue().doubleValue() / 100;
                int bottomCount = (int) Math.ceil(totalClasses * bottomPercent);
                int bottomStartIndex = Math.max(0, totalClasses - bottomCount);
                for (int i = bottomStartIndex; i < totalClasses; i++) {
                    awardedClassIds.add(classScores.get(i).getClassId());
                }
                break;

            default:
                log.warn("未知的划分方式: {}", config.getDivisionMethod());
        }

        return awardedClassIds;
    }

    /**
     * 创建评级结果
     */
    private RatingResult createRatingResult(RatingConfig config, LocalDate periodStart,
            LocalDate periodEnd, ClassScore classScore, boolean awarded) {

        RatingResult result = new RatingResult();
        result.setRatingConfigId(config.getId());
        result.setCheckPlanId(config.getCheckPlanId());
        result.setClassId(classScore.getClassId());
        result.setPeriodType(config.getRatingType());
        result.setPeriodStart(periodStart);
        result.setPeriodEnd(periodEnd);
        result.setRanking(classScore.getRanking());
        result.setFinalScore(classScore.getFinalScore());
        result.setAwarded(awarded ? 1 : 0);
        result.setStatus(config.getRequireApproval() == 1 ? "PENDING_APPROVAL" : "DRAFT");
        result.setCalculatedAt(LocalDateTime.now());

        return result;
    }

    /**
     * 保存计算明细
     */
    private void saveCalculationDetails(RatingResult result, List<DailyClassSummary> summaries) {
        for (DailyClassSummary summary : summaries) {
            RatingCalculationDetail detail = new RatingCalculationDetail();
            detail.setRatingResultId(result.getId());
            detail.setDailySummaryId(summary.getId());
            detail.setSummaryDate(summary.getSummaryDate());
            detail.setDayScore(summary.getTotalScore());
            detail.setDayWeightedScore(summary.getWeightedTotalScore());
            calculationDetailMapper.insert(detail);
        }
    }

    /**
     * 记录变更日志
     */
    private void logChange(RatingResult result, String changeType, String reason,
            String oldStatus, String newStatus) {
        RatingChangeLog log = new RatingChangeLog();
        log.setRatingResultId(result.getId());
        log.setChangeType(changeType);
        log.setChangeReason(reason);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        changeLogMapper.insert(log);
    }

    /**
     * 班级得分
     */
    private static class ClassScore {
        private Long classId;
        private BigDecimal finalScore;
        private Integer ranking;
        private List<DailyClassSummary> summaries;

        public ClassScore(Long classId, BigDecimal finalScore, List<DailyClassSummary> summaries) {
            this.classId = classId;
            this.finalScore = finalScore;
            this.summaries = summaries;
        }

        public Long getClassId() {
            return classId;
        }

        public BigDecimal getFinalScore() {
            return finalScore;
        }

        public Integer getRanking() {
            return ranking;
        }

        public void setRanking(Integer ranking) {
            this.ranking = ranking;
        }

        public List<DailyClassSummary> getSummaries() {
            return summaries;
        }
    }
}
