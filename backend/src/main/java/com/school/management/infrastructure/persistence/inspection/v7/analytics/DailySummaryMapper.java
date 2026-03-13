package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailySummaryMapper extends BaseMapper<DailySummaryPO> {

    @Select("SELECT * FROM insp_daily_summaries WHERE project_id = #{projectId} AND summary_date = #{date} " +
            "AND target_type = #{targetType} AND target_id = #{targetId} AND deleted = 0")
    DailySummaryPO findByProjectDateTarget(@Param("projectId") Long projectId,
                                            @Param("date") LocalDate date,
                                            @Param("targetType") String targetType,
                                            @Param("targetId") Long targetId);

    @Select("SELECT * FROM insp_daily_summaries WHERE project_id = #{projectId} AND summary_date = #{date} " +
            "AND deleted = 0 ORDER BY ranking")
    List<DailySummaryPO> findByProjectAndDate(@Param("projectId") Long projectId,
                                               @Param("date") LocalDate date);

    @Select("SELECT * FROM insp_daily_summaries WHERE project_id = #{projectId} " +
            "AND summary_date >= #{startDate} AND summary_date <= #{endDate} AND deleted = 0 ORDER BY summary_date, ranking")
    List<DailySummaryPO> findByProjectAndDateRange(@Param("projectId") Long projectId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM insp_daily_summaries WHERE target_type = #{targetType} AND target_id = #{targetId} " +
            "AND summary_date >= #{startDate} AND summary_date <= #{endDate} AND deleted = 0 ORDER BY summary_date")
    List<DailySummaryPO> findByTarget(@Param("targetType") String targetType,
                                       @Param("targetId") Long targetId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Delete("UPDATE insp_daily_summaries SET deleted = 1 WHERE project_id = #{projectId} AND summary_date = #{date}")
    void deleteByProjectAndDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);
}
