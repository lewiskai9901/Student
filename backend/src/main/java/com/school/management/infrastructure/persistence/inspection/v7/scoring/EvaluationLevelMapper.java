package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EvaluationLevelMapper extends BaseMapper<EvaluationLevelPO> {

    @Select("SELECT * FROM insp_evaluation_levels WHERE rule_id = #{ruleId} ORDER BY level_num")
    List<EvaluationLevelPO> findByRuleId(@Param("ruleId") Long ruleId);

    @Delete("DELETE FROM insp_evaluation_levels WHERE rule_id = #{ruleId}")
    void deleteByRuleId(@Param("ruleId") Long ruleId);
}
