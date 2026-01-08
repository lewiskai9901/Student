package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for inspection categories (DDD infrastructure).
 */
@Mapper
public interface DddInspectionCategoryMapper extends BaseMapper<InspectionCategoryPO> {

    @Select("SELECT * FROM inspection_categories WHERE template_id = #{templateId} AND deleted = 0 ORDER BY sort_order")
    List<InspectionCategoryPO> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM inspection_categories WHERE category_code = #{categoryCode} AND template_id = #{templateId} AND deleted = 0")
    InspectionCategoryPO findByCategoryCodeAndTemplateId(
        @Param("categoryCode") String categoryCode,
        @Param("templateId") Long templateId);
}
