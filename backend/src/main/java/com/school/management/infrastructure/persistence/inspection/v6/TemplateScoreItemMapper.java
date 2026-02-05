package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模板扣分项Mapper
 */
@Mapper
public interface TemplateScoreItemMapper extends BaseMapper<TemplateScoreItemPO> {

    @Select("SELECT * FROM template_score_items WHERE category_id = #{categoryId} AND deleted = 0 ORDER BY sort_order")
    List<TemplateScoreItemPO> selectByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT i.* FROM template_score_items i " +
            "INNER JOIN template_categories c ON i.category_id = c.id " +
            "WHERE c.template_id = #{templateId} AND i.deleted = 0 AND c.deleted = 0 " +
            "ORDER BY c.sort_order, i.sort_order")
    List<TemplateScoreItemPO> selectByTemplateId(@Param("templateId") Long templateId);
}
