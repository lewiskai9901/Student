package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RatingDimensionMapper extends BaseMapper<RatingDimensionPO> {

    @Select("SELECT * FROM insp_rating_dimensions WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order")
    List<RatingDimensionPO> findByProjectId(@Param("projectId") Long projectId);
}
