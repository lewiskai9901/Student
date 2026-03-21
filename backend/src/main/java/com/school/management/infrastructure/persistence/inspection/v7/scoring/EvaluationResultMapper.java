package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EvaluationResultMapper extends BaseMapper<EvaluationResultPO> {

    @Select("SELECT * FROM insp_evaluation_results WHERE rule_id = #{ruleId} AND cycle_date = #{cycleDate} AND deleted = 0")
    List<EvaluationResultPO> findByRuleIdAndCycleDate(@Param("ruleId") Long ruleId,
                                                       @Param("cycleDate") LocalDate cycleDate);

    @Update("UPDATE insp_evaluation_results SET deleted = 1 WHERE rule_id = #{ruleId} AND cycle_date = #{cycleDate}")
    void deleteByRuleIdAndCycleDate(@Param("ruleId") Long ruleId,
                                    @Param("cycleDate") LocalDate cycleDate);
}
