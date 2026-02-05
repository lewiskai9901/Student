package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 计算规则Mapper
 */
@Mapper
public interface CalculationRuleMapper extends BaseMapper<CalculationRulePO> {

    @Select("SELECT * FROM calculation_rules WHERE code = #{code} AND deleted = 0")
    CalculationRulePO selectByCode(@Param("code") String code);

    @Select("SELECT * FROM calculation_rules WHERE is_enabled = 1 AND deleted = 0 ORDER BY priority")
    List<CalculationRulePO> selectAllEnabledOrderByPriority();

    @Select("SELECT * FROM calculation_rules WHERE rule_type = #{ruleType} AND deleted = 0 ORDER BY priority")
    List<CalculationRulePO> selectByRuleType(@Param("ruleType") String ruleType);

    @Select("SELECT COUNT(*) FROM calculation_rules WHERE code = #{code} AND deleted = 0")
    int countByCode(@Param("code") String code);
}
