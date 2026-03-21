package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PolicyCalcRuleMapper extends BaseMapper<PolicyCalcRulePO> {

    @Select("SELECT * FROM insp_policy_calc_rules WHERE policy_id = #{policyId} AND deleted = 0 ORDER BY priority")
    List<PolicyCalcRulePO> findByPolicyId(@Param("policyId") Long policyId);

    @Update("UPDATE insp_policy_calc_rules SET deleted = 1 WHERE policy_id = #{policyId}")
    void deleteByPolicyId(@Param("policyId") Long policyId);
}
