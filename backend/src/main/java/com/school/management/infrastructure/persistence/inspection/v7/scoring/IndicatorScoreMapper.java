package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface IndicatorScoreMapper extends BaseMapper<IndicatorScorePO> {

    @Select("SELECT * FROM insp_indicator_scores WHERE indicator_id = #{indicatorId} AND deleted = 0 ORDER BY period_start DESC")
    List<IndicatorScorePO> findByIndicatorId(@Param("indicatorId") Long indicatorId);

    @Select("SELECT * FROM insp_indicator_scores WHERE indicator_id = #{indicatorId} AND period_start >= #{periodStart} AND period_end <= #{periodEnd} AND deleted = 0")
    List<IndicatorScorePO> findByIndicatorIdAndPeriod(@Param("indicatorId") Long indicatorId,
                                                      @Param("periodStart") LocalDate periodStart,
                                                      @Param("periodEnd") LocalDate periodEnd);

    @Select("SELECT * FROM insp_indicator_scores WHERE indicator_id = #{indicatorId} AND target_id = #{targetId} AND period_start = #{periodStart} AND deleted = 0")
    IndicatorScorePO findByIndicatorAndTargetAndPeriod(@Param("indicatorId") Long indicatorId,
                                                       @Param("targetId") Long targetId,
                                                       @Param("periodStart") LocalDate periodStart);

    @Update("UPDATE insp_indicator_scores SET deleted = 1 WHERE indicator_id = #{indicatorId} AND deleted = 0")
    void softDeleteByIndicatorId(@Param("indicatorId") Long indicatorId);
}
