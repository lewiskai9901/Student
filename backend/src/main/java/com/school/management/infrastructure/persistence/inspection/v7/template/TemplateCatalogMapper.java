package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TemplateCatalogMapper extends BaseMapper<TemplateCatalogPO> {

    @Select("SELECT * FROM insp_template_catalogs WHERE catalog_code = #{catalogCode} AND deleted = 0")
    TemplateCatalogPO findByCatalogCode(@Param("catalogCode") String catalogCode);

    @Select("SELECT * FROM insp_template_catalogs WHERE deleted = 0 ORDER BY sort_order")
    List<TemplateCatalogPO> findAllEnabled();

    @Select("SELECT * FROM insp_template_catalogs WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<TemplateCatalogPO> findByParentId(@Param("parentId") Long parentId);
}
