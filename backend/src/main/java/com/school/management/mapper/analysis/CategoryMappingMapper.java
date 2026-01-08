package com.school.management.mapper.analysis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.analysis.CategoryMapping;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 类别映射Mapper
 */
@Mapper
public interface CategoryMappingMapper extends BaseMapper<CategoryMapping> {

    /**
     * 查询配置下的类别映射列表
     */
    @Select("SELECT * FROM stat_category_mappings WHERE config_id = #{configId} ORDER BY unified_category_id, id")
    List<CategoryMapping> selectByConfigId(@Param("configId") Long configId);

    /**
     * 查询统一类别下的映射列表
     */
    @Select("SELECT * FROM stat_category_mappings WHERE config_id = #{configId} AND unified_category_id = #{unifiedCategoryId}")
    List<CategoryMapping> selectByUnifiedCategoryId(@Param("configId") Long configId, @Param("unifiedCategoryId") Long unifiedCategoryId);

    /**
     * 查询模板类别的映射
     */
    @Select("SELECT * FROM stat_category_mappings WHERE config_id = #{configId} AND template_category_id = #{templateCategoryId} LIMIT 1")
    CategoryMapping selectByTemplateCategoryId(@Param("configId") Long configId, @Param("templateCategoryId") Long templateCategoryId);

    /**
     * 统计配置下的映射数量
     */
    @Select("SELECT COUNT(*) FROM stat_category_mappings WHERE config_id = #{configId}")
    int countByConfigId(@Param("configId") Long configId);

    /**
     * 删除配置下的所有映射
     */
    @Delete("DELETE FROM stat_category_mappings WHERE config_id = #{configId}")
    void deleteByConfigId(@Param("configId") Long configId);

    /**
     * 查询配置下的统一类别列表
     */
    @Select("SELECT DISTINCT unified_category_id, unified_category_name FROM stat_category_mappings WHERE config_id = #{configId} ORDER BY unified_category_id")
    List<CategoryMapping> selectUnifiedCategories(@Param("configId") Long configId);
}
