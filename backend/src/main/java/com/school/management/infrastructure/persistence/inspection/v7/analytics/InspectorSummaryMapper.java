package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InspectorSummaryMapper extends BaseMapper<InspectorSummaryPO> {

    @Select("SELECT * FROM insp_inspector_summaries WHERE project_id = #{projectId} AND period_type = #{periodType} AND period_start = #{periodStart} AND deleted = 0 ORDER BY avg_score DESC")
    List<InspectorSummaryPO> findByProjectAndPeriod(@Param("projectId") Long projectId,
                                                     @Param("periodType") String periodType,
                                                     @Param("periodStart") LocalDate periodStart);

    @Select("SELECT * FROM insp_inspector_summaries WHERE project_id = #{projectId} AND inspector_id = #{inspectorId} AND period_type = #{periodType} AND period_start = #{periodStart} AND deleted = 0")
    InspectorSummaryPO findByInspectorAndPeriod(@Param("projectId") Long projectId,
                                                @Param("inspectorId") Long inspectorId,
                                                @Param("periodType") String periodType,
                                                @Param("periodStart") LocalDate periodStart);
}
