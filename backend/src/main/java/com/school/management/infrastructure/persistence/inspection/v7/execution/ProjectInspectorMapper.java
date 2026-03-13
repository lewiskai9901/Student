package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectInspectorMapper extends BaseMapper<ProjectInspectorPO> {

    @Select("SELECT * FROM insp_project_inspectors WHERE project_id = #{projectId} AND deleted = 0")
    List<ProjectInspectorPO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM insp_project_inspectors WHERE project_id = #{projectId} AND role = #{role} AND deleted = 0")
    List<ProjectInspectorPO> findByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role") String role);

    @Select("SELECT * FROM insp_project_inspectors WHERE user_id = #{userId} AND deleted = 0")
    List<ProjectInspectorPO> findByUserId(@Param("userId") Long userId);
}
