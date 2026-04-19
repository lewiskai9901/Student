package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课程 Mapper
 */
@Mapper
public interface CoursePersistenceMapper extends BaseMapper<CoursePO> {

    @Select("SELECT * FROM courses WHERE course_code = #{courseCode} AND deleted = 0")
    CoursePO findByCourseCode(@Param("courseCode") String courseCode);

    @Select("SELECT COUNT(*) FROM courses WHERE course_code = #{courseCode} AND deleted = 0")
    int countByCourseCode(@Param("courseCode") String courseCode);
}
