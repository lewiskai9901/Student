package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RatingResultMapper extends BaseMapper<RatingResultPO> {

    @Select("SELECT * FROM insp_rating_results WHERE dimension_id = #{dimensionId} AND deleted = 0 ORDER BY rank_no")
    List<RatingResultPO> findByDimensionId(@Param("dimensionId") Long dimensionId);

    @Select("SELECT * FROM insp_rating_results WHERE dimension_id = #{dimensionId} AND cycle_date = #{cycleDate} AND deleted = 0 ORDER BY rank_no")
    List<RatingResultPO> findByDimensionIdAndCycleDate(@Param("dimensionId") Long dimensionId,
                                                        @Param("cycleDate") LocalDate cycleDate);
}
