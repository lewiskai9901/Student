package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspProjectMapper extends BaseMapper<InspProjectPO> {

    // 单条精确查询（按项目编码）：不加数据权限，通常用于内部精确获取
    @Select("SELECT * FROM insp_projects WHERE project_code = #{projectCode} AND deleted = 0")
    InspProjectPO findByProjectCode(@Param("projectCode") String projectCode);

    /**
     * 按状态列举项目 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_project", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_projects WHERE status = #{status} AND deleted = 0")
    List<InspProjectPO> findByStatus(@Param("status") String status);

    /**
     * 按模板列举项目 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_project", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_projects WHERE template_id = #{templateId} AND deleted = 0")
    List<InspProjectPO> findByTemplateId(@Param("templateId") Long templateId);

    /**
     * 列举所有项目 — 应用数据权限过滤（最重要：列表页入口）
     */
    @DataPermission(module = "inspection_project", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_projects WHERE deleted = 0 ORDER BY created_at DESC")
    List<InspProjectPO> findAll();
}
