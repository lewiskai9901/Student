package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模板类别Mapper
 */
@Mapper
public interface TemplateCategoryMapper extends BaseMapper<TemplateCategoryPO> {

    @Select("SELECT * FROM template_categories WHERE template_id = #{templateId} AND deleted = 0 ORDER BY sort_order")
    List<TemplateCategoryPO> selectByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT COUNT(*) FROM template_categories WHERE template_id = #{templateId} AND category_code = #{code} AND deleted = 0")
    int countByTemplateIdAndCode(@Param("templateId") Long templateId, @Param("code") String code);
}
