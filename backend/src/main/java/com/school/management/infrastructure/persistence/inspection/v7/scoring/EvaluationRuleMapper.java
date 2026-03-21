package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EvaluationRuleMapper extends BaseMapper<EvaluationRulePO> {

    @Select("SELECT * FROM insp_evaluation_rules WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order")
    List<EvaluationRulePO> findByProjectId(@Param("projectId") Long projectId);
}
