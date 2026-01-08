package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckPlanRatingRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 检查计划评级规则Mapper
 *
 * @author system
 * @since 3.2.0
 */
@Mapper
public interface CheckPlanRatingRuleMapper extends BaseMapper<CheckPlanRatingRule> {

    /**
     * 根据检查计划ID获取启用的规则列表
     *
     * @param checkPlanId 检查计划ID
     * @return 规则列表
     */
    @Select("SELECT * FROM check_plan_rating_rules WHERE check_plan_id = #{checkPlanId} AND enabled = 1 AND deleted = 0 ORDER BY sort_order ASC")
    List<CheckPlanRatingRule> selectEnabledRulesByPlanId(@Param("checkPlanId") Long checkPlanId);

    /**
     * 根据检查计划ID和规则类型获取启用的规则列表
     *
     * @param checkPlanId 检查计划ID
     * @param ruleType    规则类型
     * @return 规则列表
     */
    @Select("SELECT * FROM check_plan_rating_rules WHERE check_plan_id = #{checkPlanId} AND rule_type = #{ruleType} AND enabled = 1 AND deleted = 0 ORDER BY sort_order ASC")
    List<CheckPlanRatingRule> selectEnabledRulesByPlanIdAndType(@Param("checkPlanId") Long checkPlanId, @Param("ruleType") String ruleType);
}
