package com.school.management.infrastructure.persistence.corrective;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AutoActionRuleMapper extends BaseMapper<AutoActionRulePO> {

    @Select("SELECT * FROM auto_action_rules WHERE enabled = 1 AND deleted = 0")
    List<AutoActionRulePO> findEnabled();

    @Select("SELECT * FROM auto_action_rules WHERE rule_code = #{ruleCode} AND deleted = 0")
    AutoActionRulePO findByRuleCode(@Param("ruleCode") String ruleCode);

    @Select("SELECT * FROM auto_action_rules WHERE trigger_type = #{triggerType} AND deleted = 0")
    List<AutoActionRulePO> findByTriggerType(@Param("triggerType") String triggerType);
}
