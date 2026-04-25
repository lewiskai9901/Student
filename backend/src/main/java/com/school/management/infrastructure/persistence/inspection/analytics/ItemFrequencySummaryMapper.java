package com.school.management.infrastructure.persistence.inspection.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ItemFrequencySummaryMapper extends BaseMapper<ItemFrequencySummaryPO> {

    @DataPermission(module = "inspection_summary", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_item_frequency_summaries WHERE project_id = #{projectId} AND period_type = #{periodType} AND period_start = #{periodStart} AND deleted = 0 ORDER BY occurrence_count DESC")
    List<ItemFrequencySummaryPO> findByProjectAndPeriod(@Param("projectId") Long projectId,
                                                         @Param("periodType") String periodType,
                                                         @Param("periodStart") LocalDate periodStart);

    @DataPermission(module = "inspection_summary", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_item_frequency_summaries WHERE project_id = #{projectId} AND period_type = #{periodType} AND period_start = #{periodStart} AND deleted = 0 ORDER BY total_deduction DESC LIMIT #{limit}")
    List<ItemFrequencySummaryPO> findTopNByDeduction(@Param("projectId") Long projectId,
                                                      @Param("periodType") String periodType,
                                                      @Param("periodStart") LocalDate periodStart,
                                                      @Param("limit") int limit);
}
