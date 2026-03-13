package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspTemplateMapper extends BaseMapper<InspTemplatePO> {

    @Select("SELECT * FROM insp_templates WHERE template_code = #{templateCode} AND deleted = 0")
    InspTemplatePO findByTemplateCode(@Param("templateCode") String templateCode);

    @Select("SELECT * FROM insp_templates WHERE status = #{status} AND deleted = 0 ORDER BY updated_at DESC")
    List<InspTemplatePO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM insp_templates WHERE catalog_id = #{catalogId} AND deleted = 0 ORDER BY sort_order")
    List<InspTemplatePO> findByCatalogId(@Param("catalogId") Long catalogId);

    @Select("<script>"
            + "SELECT * FROM insp_templates WHERE deleted = 0"
            + "<if test='status != null'> AND status = #{status}</if>"
            + "<if test='catalogId != null'> AND catalog_id = #{catalogId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "  AND (template_name LIKE CONCAT('%',#{keyword},'%') OR template_code LIKE CONCAT('%',#{keyword},'%'))"
            + "</if>"
            + " ORDER BY updated_at DESC"
            + " LIMIT #{size} OFFSET #{offset}"
            + "</script>")
    List<InspTemplatePO> findPagedWithConditions(@Param("offset") int offset,
                                                  @Param("size") int size,
                                                  @Param("status") String status,
                                                  @Param("catalogId") Long catalogId,
                                                  @Param("keyword") String keyword);

    @Select("<script>"
            + "SELECT COUNT(*) FROM insp_templates WHERE deleted = 0"
            + "<if test='status != null'> AND status = #{status}</if>"
            + "<if test='catalogId != null'> AND catalog_id = #{catalogId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "  AND (template_name LIKE CONCAT('%',#{keyword},'%') OR template_code LIKE CONCAT('%',#{keyword},'%'))"
            + "</if>"
            + "</script>")
    long countWithConditions(@Param("status") String status,
                             @Param("catalogId") Long catalogId,
                             @Param("keyword") String keyword);
}
