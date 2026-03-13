package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoringPresetMapper extends BaseMapper<ScoringPresetPO> {

    @Select("SELECT * FROM insp_scoring_presets WHERE template_id = #{templateId} AND deleted = 0 ORDER BY usage_count DESC, created_at DESC")
    List<ScoringPresetPO> findByTemplateId(@Param("templateId") Long templateId);
}
