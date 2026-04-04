package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GradeDefinitionMapper extends BaseMapper<GradeDefinitionPO> {

    @Select("SELECT * FROM insp_grade_definitions WHERE grade_scheme_id = #{gradeSchemeId} ORDER BY sort_order")
    List<GradeDefinitionPO> findByGradeSchemeId(@Param("gradeSchemeId") Long gradeSchemeId);

    @Delete("DELETE FROM insp_grade_definitions WHERE grade_scheme_id = #{gradeSchemeId}")
    void deleteByGradeSchemeId(@Param("gradeSchemeId") Long gradeSchemeId);
}
