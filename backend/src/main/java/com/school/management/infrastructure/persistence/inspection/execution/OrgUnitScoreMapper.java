package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OrgUnitScoreMapper extends BaseMapper<OrgUnitScorePO> {

    @Select("SELECT * FROM org_unit_scores WHERE project_id = #{projectId} AND org_unit_id = #{orgUnitId} "
            + "AND cycle_date = #{cycleDate} AND deleted = 0")
    OrgUnitScorePO findByProjectIdAndOrgUnitIdAndCycleDate(@Param("projectId") Long projectId,
                                                           @Param("orgUnitId") Long orgUnitId,
                                                           @Param("cycleDate") LocalDate cycleDate);

    @Select("SELECT * FROM org_unit_scores WHERE project_id = #{projectId} AND cycle_date = #{cycleDate} "
            + "AND deleted = 0")
    List<OrgUnitScorePO> findByProjectIdAndCycleDate(@Param("projectId") Long projectId,
                                                     @Param("cycleDate") LocalDate cycleDate);

    @Select("SELECT * FROM org_unit_scores WHERE project_id = #{projectId} AND deleted = 0 "
            + "ORDER BY cycle_date DESC")
    List<OrgUnitScorePO> findByProjectId(@Param("projectId") Long projectId);
}
