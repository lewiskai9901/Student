package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspProjectMapper extends BaseMapper<InspProjectPO> {

    @Select("SELECT * FROM insp_projects WHERE project_code = #{projectCode} AND deleted = 0")
    InspProjectPO findByProjectCode(@Param("projectCode") String projectCode);

    @Select("SELECT * FROM insp_projects WHERE status = #{status} AND deleted = 0")
    List<InspProjectPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM insp_projects WHERE template_id = #{templateId} AND deleted = 0")
    List<InspProjectPO> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM insp_projects WHERE parent_project_id = #{parentProjectId} AND deleted = 0")
    List<InspProjectPO> findByParentProjectId(@Param("parentProjectId") Long parentProjectId);

    @Select("SELECT * FROM insp_projects WHERE deleted = 0 ORDER BY created_at DESC")
    List<InspProjectPO> findAll();
}
