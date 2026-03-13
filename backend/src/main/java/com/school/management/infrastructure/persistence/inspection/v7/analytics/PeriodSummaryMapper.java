package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PeriodSummaryMapper extends BaseMapper<PeriodSummaryPO> {

    @Select("SELECT * FROM insp_period_summaries WHERE project_id = #{projectId} AND period_type = #{periodType} " +
            "AND period_start = #{periodStart} AND target_type = #{targetType} AND target_id = #{targetId} AND deleted = 0")
    PeriodSummaryPO findByProjectPeriodTarget(@Param("projectId") Long projectId,
                                               @Param("periodType") String periodType,
                                               @Param("periodStart") LocalDate periodStart,
                                               @Param("targetType") String targetType,
                                               @Param("targetId") Long targetId);

    @Select("SELECT * FROM insp_period_summaries WHERE project_id = #{projectId} AND period_type = #{periodType} " +
            "AND period_start = #{periodStart} AND deleted = 0 ORDER BY ranking")
    List<PeriodSummaryPO> findByProjectAndPeriod(@Param("projectId") Long projectId,
                                                   @Param("periodType") String periodType,
                                                   @Param("periodStart") LocalDate periodStart);

    @Select("SELECT * FROM insp_period_summaries WHERE target_type = #{targetType} AND target_id = #{targetId} " +
            "AND period_type = #{periodType} AND deleted = 0 ORDER BY period_start DESC")
    List<PeriodSummaryPO> findByTarget(@Param("targetType") String targetType,
                                        @Param("targetId") Long targetId,
                                        @Param("periodType") String periodType);

    @Delete("UPDATE insp_period_summaries SET deleted = 1 WHERE project_id = #{projectId} " +
            "AND period_type = #{periodType} AND period_start = #{periodStart}")
    void deleteByProjectAndPeriod(@Param("projectId") Long projectId,
                                   @Param("periodType") String periodType,
                                   @Param("periodStart") LocalDate periodStart);
}
