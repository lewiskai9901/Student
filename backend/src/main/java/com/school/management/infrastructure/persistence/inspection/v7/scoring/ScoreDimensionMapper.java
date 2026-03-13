package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoreDimensionMapper extends BaseMapper<ScoreDimensionPO> {

    @Select("SELECT * FROM insp_score_dimensions WHERE scoring_profile_id = #{scoringProfileId} AND deleted = 0 ORDER BY sort_order")
    List<ScoreDimensionPO> findByScoringProfileId(@Param("scoringProfileId") Long scoringProfileId);
}
