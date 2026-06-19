package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
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

    /**
     * 按 (project, cycleDate) 列举各组织 roll-up 得分 — 应用数据权限过滤.
     * 读端点 GET /inspection/org-scores 走这条; 范围受限用户只能看到自己范围内的组织得分.
     */
    @DataPermission(module = "inspection_project")
    @Select("SELECT * FROM org_unit_scores WHERE project_id = #{projectId} AND cycle_date = #{cycleDate} "
            + "AND deleted = 0")
    List<OrgUnitScorePO> findByProjectIdAndCycleDate(@Param("projectId") Long projectId,
                                                     @Param("cycleDate") LocalDate cycleDate);

    /**
     * 按 project 列举各组织 roll-up 得分 — 应用数据权限过滤 (list 查询统一加).
     */
    @DataPermission(module = "inspection_project")
    @Select("SELECT * FROM org_unit_scores WHERE project_id = #{projectId} AND deleted = 0 "
            + "ORDER BY cycle_date DESC")
    List<OrgUnitScorePO> findByProjectId(@Param("projectId") Long projectId);
}
