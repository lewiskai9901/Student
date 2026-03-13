package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TemplateItemMapper extends BaseMapper<TemplateItemPO> {

    @Select("SELECT * FROM insp_template_items WHERE section_id = #{sectionId} AND deleted = 0 ORDER BY sort_order")
    List<TemplateItemPO> findBySectionId(@Param("sectionId") Long sectionId);

    @Select("SELECT ti.* FROM insp_template_items ti "
            + "INNER JOIN insp_template_sections ts ON ti.section_id = ts.id "
            + "WHERE ts.template_id = #{templateId} AND ti.deleted = 0 AND ts.deleted = 0 "
            + "ORDER BY ts.sort_order, ti.sort_order")
    List<TemplateItemPO> findByTemplateId(@Param("templateId") Long templateId);

    @Update("UPDATE insp_template_items SET deleted = 1 WHERE section_id = #{sectionId} AND deleted = 0")
    void softDeleteBySectionId(@Param("sectionId") Long sectionId);

    @Select("SELECT * FROM insp_template_items WHERE library_item_id = #{libraryItemId} AND sync_with_library = 1 AND deleted = 0")
    List<TemplateItemPO> findByLibraryItemIdWithSync(@Param("libraryItemId") Long libraryItemId);
}
