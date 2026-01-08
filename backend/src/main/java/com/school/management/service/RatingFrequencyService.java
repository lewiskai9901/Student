package com.school.management.service;

import com.school.management.dto.rating.RatingFrequencyQueryDTO;
import com.school.management.dto.rating.RatingFrequencySummaryVO;
import com.school.management.dto.rating.RatingFrequencyVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级频次统计服务接口
 */
public interface RatingFrequencyService {

    /**
     * 计算并更新指定周期的评级频次统计
     *
     * @param checkPlanId 检查计划ID
     * @param periodType  周期类型
     * @param periodStart 周期开始日期
     * @param periodEnd   周期结束日期
     */
    void calculateFrequency(Long checkPlanId, String periodType, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 计算当前周期的评级频次（自动推断周期）
     *
     * @param checkPlanId 检查计划ID
     * @param periodType  周期类型 (WEEK, MONTH, etc.)
     */
    void calculateCurrentPeriodFrequency(Long checkPlanId, String periodType);

    /**
     * 查询评级频次列表
     *
     * @param query 查询条件
     * @return 频次列表
     */
    List<RatingFrequencyVO> queryFrequency(RatingFrequencyQueryDTO query);

    /**
     * 获取评级频次汇总统计
     *
     * @param checkPlanId 检查计划ID
     * @param ruleId      规则ID（可选）
     * @param periodType  周期类型
     * @param periodStart 周期开始日期
     * @param periodEnd   周期结束日期
     * @return 汇总统计
     */
    RatingFrequencySummaryVO getFrequencySummary(Long checkPlanId, Long ruleId,
                                                   String periodType, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 获取班级的评级频次历史
     *
     * @param classId    班级ID
     * @param ruleId     规则ID
     * @param periodType 周期类型
     * @param limit      返回数量限制
     * @return 历史记录
     */
    List<RatingFrequencyVO> getClassFrequencyHistory(Long classId, Long ruleId, String periodType, Integer limit);

    /**
     * 获取指定等级的TOP班级
     *
     * @param ruleId      规则ID
     * @param levelId     等级ID
     * @param periodType  周期类型
     * @param periodStart 周期开始日期
     * @param topN        TOP数量
     * @return TOP班级列表
     */
    List<RatingFrequencyVO> getTopClassesByLevel(Long ruleId, Long levelId,
                                                   String periodType, LocalDate periodStart, int topN);
}
