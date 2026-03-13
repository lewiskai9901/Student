package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CorrectiveSummaryMapper extends BaseMapper<CorrectiveSummaryPO> {

    @Select("SELECT * FROM insp_corrective_summaries WHERE project_id = #{projectId} AND period_type = #{periodType} AND period_start = #{periodStart} AND deleted = 0")
    CorrectiveSummaryPO findByProjectAndPeriod(@Param("projectId") Long projectId,
                                                @Param("periodType") String periodType,
                                                @Param("periodStart") LocalDate periodStart);

    @Select("SELECT * FROM insp_corrective_summaries WHERE project_id = #{projectId} AND deleted = 0 ORDER BY period_start DESC")
    List<CorrectiveSummaryPO> findByProject(@Param("projectId") Long projectId);
}
