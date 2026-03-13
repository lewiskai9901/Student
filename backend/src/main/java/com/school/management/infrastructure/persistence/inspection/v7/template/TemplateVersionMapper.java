package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TemplateVersionMapper extends BaseMapper<TemplateVersionPO> {

    @Select("SELECT * FROM insp_template_versions WHERE template_id = #{templateId} AND version = #{version} AND deleted = 0")
    TemplateVersionPO findByTemplateIdAndVersion(@Param("templateId") Long templateId,
                                                  @Param("version") Integer version);

    @Select("SELECT * FROM insp_template_versions WHERE template_id = #{templateId} AND deleted = 0 ORDER BY version DESC")
    List<TemplateVersionPO> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM insp_template_versions WHERE template_id = #{templateId} AND deleted = 0 ORDER BY version DESC LIMIT 1")
    TemplateVersionPO findLatestByTemplateId(@Param("templateId") Long templateId);
}
