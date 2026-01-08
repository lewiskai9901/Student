package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckPlanRatingResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 检查计划评级结果Mapper
 *
 * @author system
 * @since 3.2.0
 */
@Mapper
public interface CheckPlanRatingResultMapper extends BaseMapper<CheckPlanRatingResult> {

    /**
     * 根据规则ID和检查记录ID获取评级结果
     *
     * @param ruleId        规则ID
     * @param checkRecordId 检查记录ID
     * @return 评级结果列表
     */
    @Select("SELECT * FROM check_plan_rating_results WHERE rule_id = #{ruleId} AND check_record_id = #{checkRecordId} ORDER BY ranking ASC")
    List<CheckPlanRatingResult> selectByRuleAndRecord(@Param("ruleId") Long ruleId, @Param("checkRecordId") Long checkRecordId);

    /**
     * 根据规则ID和时间段获取汇总评级结果
     *
     * @param ruleId      规则ID
     * @param periodStart 周期开始
     * @param periodEnd   周期结束
     * @return 评级结果列表
     */
    @Select("SELECT * FROM check_plan_rating_results WHERE rule_id = #{ruleId} AND period_start = #{periodStart} AND period_end = #{periodEnd} ORDER BY ranking ASC")
    List<CheckPlanRatingResult> selectSummaryByRuleAndPeriod(@Param("ruleId") Long ruleId,
                                                             @Param("periodStart") LocalDate periodStart,
                                                             @Param("periodEnd") LocalDate periodEnd);

    /**
     * 删除指定规则和检查记录的评级结果
     *
     * @param ruleId        规则ID
     * @param checkRecordId 检查记录ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM check_plan_rating_results WHERE rule_id = #{ruleId} AND check_record_id = #{checkRecordId}")
    int deleteByRuleAndRecord(@Param("ruleId") Long ruleId, @Param("checkRecordId") Long checkRecordId);

    /**
     * 删除指定规则和时间段的汇总评级结果
     *
     * @param ruleId      规则ID
     * @param periodStart 周期开始
     * @param periodEnd   周期结束
     * @return 删除的行数
     */
    @Delete("DELETE FROM check_plan_rating_results WHERE rule_id = #{ruleId} AND period_start = #{periodStart} AND period_end = #{periodEnd}")
    int deleteSummaryByRuleAndPeriod(@Param("ruleId") Long ruleId,
                                      @Param("periodStart") LocalDate periodStart,
                                      @Param("periodEnd") LocalDate periodEnd);

    /**
     * 根据规则ID删除所有评级结果
     *
     * @param ruleId 规则ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM check_plan_rating_results WHERE rule_id = #{ruleId}")
    int deleteByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 根据检查记录ID获取所有评级结果
     *
     * @param checkRecordId 检查记录ID
     * @return 评级结果列表
     */
    @Select("SELECT * FROM check_plan_rating_results WHERE check_record_id = #{checkRecordId} ORDER BY rule_id, ranking")
    List<CheckPlanRatingResult> selectByRecordId(@Param("checkRecordId") Long checkRecordId);

    /**
     * 根据班级ID和检查计划ID获取评级历史
     *
     * @param classId     班级ID
     * @param checkPlanId 检查计划ID
     * @return 评级结果列表
     */
    @Select("SELECT * FROM check_plan_rating_results WHERE class_id = #{classId} AND check_plan_id = #{checkPlanId} ORDER BY created_at DESC")
    List<CheckPlanRatingResult> selectByClassAndPlan(@Param("classId") Long classId, @Param("checkPlanId") Long checkPlanId);
}
