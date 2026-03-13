package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TemplateModuleRefMapper extends BaseMapper<TemplateModuleRefPO> {

    @Select("SELECT * FROM insp_template_module_refs WHERE composite_template_id = #{compositeTemplateId} AND deleted = 0 ORDER BY sort_order")
    List<TemplateModuleRefPO> findByCompositeTemplateId(@Param("compositeTemplateId") Long compositeTemplateId);

    @Update("UPDATE insp_template_module_refs SET deleted = 1 WHERE composite_template_id = #{compositeTemplateId} AND deleted = 0")
    void softDeleteByCompositeTemplateId(@Param("compositeTemplateId") Long compositeTemplateId);
}
