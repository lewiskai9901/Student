package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.TemplateCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 模板类别Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface TemplateCategoryMapper extends BaseMapper<TemplateCategory> {

    /**
     * 物理删除模板关联的类别
     * 由于template_categories表有唯一约束(template_id, category_id)，
     * 而MyBatis Plus的@TableLogic只做软删除，会导致更新时唯一约束冲突，
     * 所以需要使用物理删除
     */
    @Delete("DELETE FROM template_categories WHERE template_id = #{templateId}")
    int physicalDeleteByTemplateId(@Param("templateId") Long templateId);
}
