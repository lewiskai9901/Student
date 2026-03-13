package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TemplateSectionMapper extends BaseMapper<TemplateSectionPO> {

    @Select("SELECT * FROM insp_template_sections WHERE template_id = #{templateId} AND deleted = 0 ORDER BY sort_order")
    List<TemplateSectionPO> findByTemplateId(@Param("templateId") Long templateId);

    @Update("UPDATE insp_template_sections SET deleted = 1 WHERE template_id = #{templateId} AND deleted = 0")
    void softDeleteByTemplateId(@Param("templateId") Long templateId);
}
