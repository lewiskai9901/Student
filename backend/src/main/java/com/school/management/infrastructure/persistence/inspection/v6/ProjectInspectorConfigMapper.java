package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * V6项目检查员配置Mapper
 */
@Mapper
public interface ProjectInspectorConfigMapper extends BaseMapper<ProjectInspectorConfigPO> {

    @Select("SELECT * FROM project_inspector_configs WHERE project_id = #{projectId}")
    List<ProjectInspectorConfigPO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM project_inspector_configs WHERE project_id = #{projectId} AND is_default = 1")
    List<ProjectInspectorConfigPO> findDefaultInspectors(@Param("projectId") Long projectId);

    @Select("SELECT * FROM project_inspector_configs WHERE project_id = #{projectId} AND inspector_id = #{inspectorId}")
    ProjectInspectorConfigPO findByProjectAndInspector(@Param("projectId") Long projectId, @Param("inspectorId") Long inspectorId);

    @Delete("DELETE FROM project_inspector_configs WHERE project_id = #{projectId}")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Delete("DELETE FROM project_inspector_configs WHERE project_id = #{projectId} AND inspector_id = #{inspectorId}")
    void deleteByProjectAndInspector(@Param("projectId") Long projectId, @Param("inspectorId") Long inspectorId);

    @Insert("<script>" +
            "INSERT INTO project_inspector_configs (project_id, inspector_id, inspector_name, is_default, scope_type, scope_ids, available_days, available_time_slots, created_by, created_at) VALUES " +
            "<foreach collection='configs' item='c' separator=','>" +
            "(#{c.projectId}, #{c.inspectorId}, #{c.inspectorName}, #{c.isDefault}, #{c.scopeType}, #{c.scopeIds}, #{c.availableDays}, #{c.availableTimeSlots}, #{c.createdBy}, NOW())" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("configs") List<ProjectInspectorConfigPO> configs);
}
