package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoringPolicyMapper extends BaseMapper<ScoringPolicyPO> {

    @Select("SELECT * FROM insp_scoring_policies WHERE policy_code = #{policyCode} AND deleted = 0")
    ScoringPolicyPO findByCode(@Param("policyCode") String policyCode);

    @Select("SELECT * FROM insp_scoring_policies WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<ScoringPolicyPO> findEnabled();
}
