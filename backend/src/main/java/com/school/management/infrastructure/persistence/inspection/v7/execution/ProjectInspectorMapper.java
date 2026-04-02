package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectInspectorMapper extends BaseMapper<ProjectInspectorPO> {

    /**
     * 按项目查询检查员列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_project_inspectors WHERE project_id = #{projectId} AND deleted = 0")
    List<ProjectInspectorPO> findByProjectId(@Param("projectId") Long projectId);

    /**
     * 按项目和角色查询检查员列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_project_inspectors WHERE project_id = #{projectId} AND role = #{role} AND deleted = 0")
    List<ProjectInspectorPO> findByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role") String role);

    /**
     * 按用户查询检查员列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_project_inspectors WHERE user_id = #{userId} AND deleted = 0")
    List<ProjectInspectorPO> findByUserId(@Param("userId") Long userId);
}
