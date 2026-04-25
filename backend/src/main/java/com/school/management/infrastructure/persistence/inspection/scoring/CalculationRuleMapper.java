package com.school.management.infrastructure.persistence.inspection.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CalculationRuleMapper extends BaseMapper<CalculationRulePO> {

    @Select("SELECT * FROM insp_calculation_rules WHERE scoring_profile_id = #{scoringProfileId} AND deleted = 0 ORDER BY priority")
    List<CalculationRulePO> findByScoringProfileIdOrderByPriority(@Param("scoringProfileId") Long scoringProfileId);

    @Select("SELECT * FROM insp_calculation_rules WHERE scoring_profile_id = #{scoringProfileId} AND deleted = 0")
    List<CalculationRulePO> findByScoringProfileId(@Param("scoringProfileId") Long scoringProfileId);
}
