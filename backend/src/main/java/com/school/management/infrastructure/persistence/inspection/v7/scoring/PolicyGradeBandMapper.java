package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PolicyGradeBandMapper extends BaseMapper<PolicyGradeBandPO> {

    @Select("SELECT * FROM insp_policy_grade_bands WHERE policy_id = #{policyId} AND deleted = 0 ORDER BY sort_order")
    List<PolicyGradeBandPO> findByPolicyId(@Param("policyId") Long policyId);

    @Update("UPDATE insp_policy_grade_bands SET deleted = 1 WHERE policy_id = #{policyId}")
    void deleteByPolicyId(@Param("policyId") Long policyId);
}
