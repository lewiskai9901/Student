package com.school.management.service.rating.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.entity.rating.DailyClassSummary;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import com.school.management.mapper.record.CheckRecordMapper;
import com.school.management.mapper.record.CheckRecordClassStatsMapper;
import com.school.management.mapper.rating.DailyClassSummaryMapper;
import com.school.management.service.rating.DailyClassSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 每日班级汇总服务实现
 *
 * @author System
 * @since 4.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyClassSummaryServiceImpl extends ServiceImpl<DailyClassSummaryMapper, DailyClassSummary>
        implements DailyClassSummaryService {

    private final CheckRecordMapper checkRecordMapper;
    private final CheckRecordClassStatsMapper classStatsMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void summarizeDaily(Long checkPlanId, LocalDate summaryDate) {
        log.info("开始汇总每日检查结果: checkPlanId={}, summaryDate={}", checkPlanId, summaryDate);

        // 1. 查询当日的检查记录
        LambdaQueryWrapper<CheckRecordNew> recordQuery = new LambdaQueryWrapper<>();
        recordQuery.eq(CheckRecordNew::getPlanId, checkPlanId)
                .eq(CheckRecordNew::getCheckDate, summaryDate);
        List<CheckRecordNew> records = checkRecordMapper.selectList(recordQuery);

        if (records.isEmpty()) {
            log.info("当日无检查记录，跳过汇总");
            return;
        }

        // 2. 获取所有记录的ID
        List<Long> recordIds = records.stream()
                .map(CheckRecordNew::getId)
                .collect(Collectors.toList());

        // 3. 查询所有班级统计数据
        LambdaQueryWrapper<CheckRecordClassStatsNew> statsQuery = new LambdaQueryWrapper<>();
        statsQuery.in(CheckRecordClassStatsNew::getRecordId, recordIds);
        List<CheckRecordClassStatsNew> allStats = classStatsMapper.selectList(statsQuery);

        if (allStats.isEmpty()) {
            log.info("无班级统计数据");
            return;
        }

        // 4. 按班级分组汇总
        Map<Long, List<CheckRecordClassStatsNew>> statsByClass = allStats.stream()
                .collect(Collectors.groupingBy(CheckRecordClassStatsNew::getClassId));

        // 5. 对每个班级进行汇总
        for (Map.Entry<Long, List<CheckRecordClassStatsNew>> entry : statsByClass.entrySet()) {
            Long classId = entry.getKey();
            List<CheckRecordClassStatsNew> classStats = entry.getValue();

            // 计算总分（扣分相加）
            BigDecimal totalScore = classStats.stream()
                    .map(CheckRecordClassStatsNew::getTotalScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算加权总分（暂时与总分一致）
            BigDecimal weightedTotalScore = totalScore;

            // 计算分类得分
            Map<String, BigDecimal> categoryScores = new HashMap<>();
            categoryScores.put("hygiene", sumScore(classStats, CheckRecordClassStatsNew::getHygieneScore));
            categoryScores.put("discipline", sumScore(classStats, CheckRecordClassStatsNew::getDisciplineScore));
            categoryScores.put("attendance", sumScore(classStats, CheckRecordClassStatsNew::getAttendanceScore));
            categoryScores.put("dormitory", sumScore(classStats, CheckRecordClassStatsNew::getDormitoryScore));
            categoryScores.put("other", sumScore(classStats, CheckRecordClassStatsNew::getOtherScore));

            // 计算扣分项得分（这里简化处理，实际可以扩展）
            Map<String, BigDecimal> itemScores = new HashMap<>();

            // 6. 保存或更新汇总记录
            saveSummary(checkPlanId, classId, summaryDate, totalScore, weightedTotalScore,
                    categoryScores, itemScores, classStats.size());
        }

        log.info("每日汇总完成: checkPlanId={}, summaryDate={}, 班级数={}",
                checkPlanId, summaryDate, statsByClass.size());
    }

    @Override
    @Transactional
    public void resynchronizeDaily(Long checkPlanId, LocalDate summaryDate) {
        log.info("重新汇总每日检查结果: checkPlanId={}, summaryDate={}", checkPlanId, summaryDate);

        // 删除已有汇总
        LambdaQueryWrapper<DailyClassSummary> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(DailyClassSummary::getCheckPlanId, checkPlanId)
                .eq(DailyClassSummary::getSummaryDate, summaryDate);
        this.remove(deleteQuery);

        // 重新汇总
        summarizeDaily(checkPlanId, summaryDate);
    }

    @Override
    public List<DailyClassSummary> getDailySummaries(Long checkPlanId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DailyClassSummary> query = new LambdaQueryWrapper<>();
        query.eq(DailyClassSummary::getCheckPlanId, checkPlanId)
                .ge(DailyClassSummary::getSummaryDate, startDate)
                .le(DailyClassSummary::getSummaryDate, endDate)
                .orderByAsc(DailyClassSummary::getSummaryDate, DailyClassSummary::getClassId);
        return this.list(query);
    }

    @Override
    public List<DailyClassSummary> getClassDailySummaries(Long checkPlanId, Long classId,
            LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DailyClassSummary> query = new LambdaQueryWrapper<>();
        query.eq(DailyClassSummary::getCheckPlanId, checkPlanId)
                .eq(DailyClassSummary::getClassId, classId)
                .ge(DailyClassSummary::getSummaryDate, startDate)
                .le(DailyClassSummary::getSummaryDate, endDate)
                .orderByAsc(DailyClassSummary::getSummaryDate);
        return this.list(query);
    }

    /**
     * 求和分数
     */
    private BigDecimal sumScore(List<CheckRecordClassStatsNew> stats,
                                 java.util.function.Function<CheckRecordClassStatsNew, BigDecimal> getter) {
        return stats.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 保存汇总记录
     */
    private void saveSummary(Long checkPlanId, Long classId, LocalDate summaryDate,
            BigDecimal totalScore, BigDecimal weightedTotalScore,
            Map<String, BigDecimal> categoryScores, Map<String, BigDecimal> itemScores,
            int checkCount) {

        // 查找已有记录
        LambdaQueryWrapper<DailyClassSummary> query = new LambdaQueryWrapper<>();
        query.eq(DailyClassSummary::getCheckPlanId, checkPlanId)
                .eq(DailyClassSummary::getClassId, classId)
                .eq(DailyClassSummary::getSummaryDate, summaryDate);
        DailyClassSummary summary = this.getOne(query);

        if (summary == null) {
            summary = new DailyClassSummary();
            summary.setCheckPlanId(checkPlanId);
            summary.setClassId(classId);
            summary.setSummaryDate(summaryDate);
        }

        summary.setTotalScore(totalScore);
        summary.setWeightedTotalScore(weightedTotalScore);
        summary.setCheckCount(checkCount);

        try {
            summary.setCategoryScores(objectMapper.writeValueAsString(categoryScores));
            summary.setItemScores(objectMapper.writeValueAsString(itemScores));
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            summary.setCategoryScores("{}");
            summary.setItemScores("{}");
        }

        this.saveOrUpdate(summary);
    }
}
