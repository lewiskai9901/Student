package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GradeBandMapper extends BaseMapper<GradeBandPO> {

    @Select("SELECT * FROM insp_grade_bands WHERE scoring_profile_id = #{scoringProfileId} AND deleted = 0 ORDER BY sort_order")
    List<GradeBandPO> findByScoringProfileId(@Param("scoringProfileId") Long scoringProfileId);

    @Select("SELECT * FROM insp_grade_bands WHERE dimension_id = #{dimensionId} AND deleted = 0 ORDER BY sort_order")
    List<GradeBandPO> findByDimensionId(@Param("dimensionId") Long dimensionId);
}
