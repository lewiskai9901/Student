package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScoringProfileMapper extends BaseMapper<ScoringProfilePO> {

    @Select("SELECT * FROM insp_scoring_profiles WHERE template_id = #{templateId} AND deleted = 0")
    ScoringProfilePO findByTemplateId(@Param("templateId") Long templateId);
}
