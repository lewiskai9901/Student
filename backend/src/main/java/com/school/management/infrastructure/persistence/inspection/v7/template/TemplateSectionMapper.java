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

    @Select("SELECT * FROM insp_template_sections WHERE template_id IS NULL AND parent_section_id IS NULL AND deleted = 0 ORDER BY sort_order")
    List<TemplateSectionPO> findAllRootSections();

    @Select("<script>"
            + "SELECT * FROM insp_template_sections"
            + " WHERE template_id IS NULL AND parent_section_id IS NULL AND deleted = 0"
            + "<if test='status != null'> AND status = #{status}</if>"
            + "<if test='catalogId != null'> AND catalog_id = #{catalogId}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND section_name LIKE CONCAT('%', #{keyword}, '%')</if>"
            + " ORDER BY sort_order"
            + " LIMIT #{offset}, #{size}"
            + "</script>")
    List<TemplateSectionPO> findRootSectionsPaged(@Param("offset") int offset,
                                                   @Param("size") int size,
                                                   @Param("status") String status,
                                                   @Param("catalogId") Long catalogId,
                                                   @Param("keyword") String keyword);

    @Select("<script>"
            + "SELECT COUNT(*) FROM insp_template_sections"
            + " WHERE template_id IS NULL AND parent_section_id IS NULL AND deleted = 0"
            + "<if test='status != null'> AND status = #{status}</if>"
            + "<if test='catalogId != null'> AND catalog_id = #{catalogId}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND section_name LIKE CONCAT('%', #{keyword}, '%')</if>"
            + "</script>")
    int countRootSections(@Param("status") String status,
                          @Param("catalogId") Long catalogId,
                          @Param("keyword") String keyword);
}
