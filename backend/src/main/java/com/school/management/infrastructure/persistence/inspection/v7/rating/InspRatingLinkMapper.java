package com.school.management.infrastructure.persistence.inspection.v7.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspRatingLinkMapper extends BaseMapper<InspRatingLinkPO> {

    @Select("SELECT * FROM insp_rating_links WHERE project_id = #{projectId} AND deleted = 0")
    List<InspRatingLinkPO> findByProjectId(Long projectId);

    @Select("SELECT * FROM insp_rating_links WHERE project_id = #{projectId} AND period_type = #{periodType} AND deleted = 0")
    List<InspRatingLinkPO> findByProjectIdAndPeriodType(Long projectId, String periodType);

    @Select("SELECT * FROM insp_rating_links WHERE rating_config_id = #{ratingConfigId} AND deleted = 0")
    List<InspRatingLinkPO> findByRatingConfigId(Long ratingConfigId);
}
