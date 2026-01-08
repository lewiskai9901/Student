package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckPlanRatingLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 检查计划评级等级Mapper
 *
 * @author system
 * @since 3.2.0
 */
@Mapper
public interface CheckPlanRatingLevelMapper extends BaseMapper<CheckPlanRatingLevel> {

    /**
     * 根据规则ID获取等级列表（按顺序排序）
     *
     * @param ruleId 规则ID
     * @return 等级列表
     */
    @Select("SELECT * FROM check_plan_rating_levels WHERE rule_id = #{ruleId} ORDER BY level_order ASC")
    List<CheckPlanRatingLevel> selectByRuleIdOrdered(@Param("ruleId") Long ruleId);

    /**
     * 根据规则ID删除所有等级
     *
     * @param ruleId 规则ID
     * @return 删除的行数
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM check_plan_rating_levels WHERE rule_id = #{ruleId}")
    int deleteByRuleId(@Param("ruleId") Long ruleId);
}
