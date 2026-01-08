package com.school.management.service;

import com.school.management.dto.rating.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 检查计划评级服务接口
 *
 * @author system
 * @since 3.2.0
 */
public interface CheckPlanRatingService {

    // ==================== 规则管理 ====================

    /**
     * 创建评级规则
     *
     * @param dto 创建参数
     * @return 规则ID
     */
    Long createRule(RatingRuleCreateDTO dto);

    /**
     * 更新评级规则
     *
     * @param ruleId 规则ID
     * @param dto    更新参数
     */
    void updateRule(Long ruleId, RatingRuleUpdateDTO dto);

    /**
     * 删除评级规则
     *
     * @param ruleId 规则ID
     */
    void deleteRule(Long ruleId);

    /**
     * 获取规则详情
     *
     * @param ruleId 规则ID
     * @return 规则详情
     */
    RatingRuleVO getRuleDetail(Long ruleId);

    /**
     * 获取检查计划的所有评级规则
     *
     * @param checkPlanId 检查计划ID
     * @return 规则列表
     */
    List<RatingRuleVO> getRulesByPlanId(Long checkPlanId);

    /**
     * 启用/禁用规则
     *
     * @param ruleId  规则ID
     * @param enabled 是否启用
     */
    void toggleRuleEnabled(Long ruleId, boolean enabled);

    // ==================== 评级计算 ====================

    /**
     * 计算单次检查评级（DAILY类型）
     *
     * @param checkRecordId    检查记录ID
     * @param ruleId           规则ID（可选，为空则计算所有启用的DAILY规则）
     * @param forceRecalculate 是否强制重新计算
     * @return 评级结果列表
     */
    List<RatingResultVO> calculateDailyRating(Long checkRecordId, Long ruleId, boolean forceRecalculate);

    /**
     * 计算汇总评级（SUMMARY类型）
     *
     * @param checkPlanId      检查计划ID
     * @param periodStart      周期开始（可选，默认为计划开始日期）
     * @param periodEnd        周期结束（可选，默认为计划结束日期或当前日期）
     * @param ruleId           规则ID（可选，为空则计算所有启用的SUMMARY规则）
     * @param forceRecalculate 是否强制重新计算
     * @return 评级结果列表
     */
    List<RatingResultVO> calculateSummaryRating(Long checkPlanId, LocalDate periodStart, LocalDate periodEnd,
                                                 Long ruleId, boolean forceRecalculate);

    /**
     * 重新计算指定规则的所有评级结果
     * 用于规则修改后重新计算历史结果
     *
     * @param ruleId 规则ID
     */
    void recalculateAllByRule(Long ruleId);

    // ==================== 结果查询 ====================

    /**
     * 获取单次检查的评级结果
     *
     * @param checkRecordId 检查记录ID
     * @return 评级结果列表（按规则分组）
     */
    List<RatingResultVO> getResultsByRecord(Long checkRecordId);

    /**
     * 获取检查计划的汇总评级结果
     *
     * @param checkPlanId 检查计划ID
     * @param periodStart 周期开始（可选）
     * @param periodEnd   周期结束（可选）
     * @return 评级结果列表
     */
    List<RatingResultVO> getSummaryResults(Long checkPlanId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 获取班级的评级历史
     *
     * @param classId     班级ID
     * @param checkPlanId 检查计划ID
     * @return 评级结果列表
     */
    List<RatingResultVO> getClassRatingHistory(Long classId, Long checkPlanId);

    /**
     * 获取评级统计信息
     *
     * @param ruleId        规则ID
     * @param checkRecordId 检查记录ID（DAILY类型时使用）
     * @param periodStart   周期开始（SUMMARY类型时使用）
     * @param periodEnd     周期结束（SUMMARY类型时使用）
     * @return 统计信息
     */
    RatingStatisticsVO getRatingStatistics(Long ruleId, Long checkRecordId, LocalDate periodStart, LocalDate periodEnd);

    // ==================== 审核管理 ====================

    /**
     * 审核评级结果
     *
     * @param dto 审核参数
     */
    void approveResults(RatingApprovalDTO dto);

    /**
     * 修改评级结果
     *
     * @param dto 修改参数
     */
    void modifyResult(RatingModifyDTO dto);

    /**
     * 发布评级结果
     *
     * @param resultId 结果ID
     */
    void publishResult(Long resultId);

    /**
     * 取消发布评级结果
     *
     * @param resultId 结果ID
     */
    void unpublishResult(Long resultId);

    /**
     * 批量发布检查记录的所有评级结果
     *
     * @param checkRecordId 检查记录ID
     */
    void publishAllByRecord(Long checkRecordId);

    /**
     * 获取待审核的评级结果
     *
     * @param checkPlanId   检查计划ID（可选）
     * @param checkRecordId 检查记录ID（可选）
     * @return 待审核结果列表
     */
    List<RatingResultVO> getPendingResults(Long checkPlanId, Long checkRecordId);
}
