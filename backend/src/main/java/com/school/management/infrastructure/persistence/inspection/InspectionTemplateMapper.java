package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for inspection templates.
 * Maps to the existing 'check_templates' table.
 */
@Mapper
public interface InspectionTemplateMapper extends BaseMapper<InspectionTemplatePO> {

    @Select("SELECT * FROM check_templates WHERE template_code = #{templateCode} AND deleted = 0")
    InspectionTemplatePO findByTemplateCode(@Param("templateCode") String templateCode);

    @Select("SELECT * FROM check_templates WHERE status = #{status} AND deleted = 0")
    List<InspectionTemplatePO> findByStatus(@Param("status") Integer status);

    @Select("SELECT * FROM check_templates WHERE is_default = 1 AND deleted = 0 LIMIT 1")
    InspectionTemplatePO findDefault();

    @Select("SELECT * FROM check_templates WHERE status = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionTemplatePO> findAllEnabled();

    @Select("SELECT COUNT(*) > 0 FROM check_templates WHERE template_code = #{templateCode} AND deleted = 0")
    boolean existsByTemplateCode(@Param("templateCode") String templateCode);
}
