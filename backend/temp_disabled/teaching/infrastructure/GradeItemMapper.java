package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GradeItemMapper extends BaseMapper<GradeItemPO> {
    @Delete("DELETE FROM student_grade_items WHERE grade_id = #{gradeId}")
    void deleteByGradeId(@Param("gradeId") Long gradeId);
}
