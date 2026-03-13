package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CalculationRuleV7Mapper extends BaseMapper<CalculationRuleV7PO> {

    @Select("SELECT * FROM insp_calculation_rules WHERE scoring_profile_id = #{scoringProfileId} AND deleted = 0 ORDER BY priority")
    List<CalculationRuleV7PO> findByScoringProfileIdOrderByPriority(@Param("scoringProfileId") Long scoringProfileId);

    @Select("SELECT * FROM insp_calculation_rules WHERE scoring_profile_id = #{scoringProfileId} AND deleted = 0")
    List<CalculationRuleV7PO> findByScoringProfileId(@Param("scoringProfileId") Long scoringProfileId);
}
