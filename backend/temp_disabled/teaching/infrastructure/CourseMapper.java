package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课程Mapper
 */
@Mapper
public interface CourseMapper extends BaseMapper<CoursePO> {

    /**
     * 根据关键词搜索课程
     */
    @Select("SELECT * FROM courses WHERE course_code LIKE CONCAT('%',#{keyword},'%') " +
            "OR course_name LIKE CONCAT('%',#{keyword},'%') " +
            "ORDER BY course_code")
    List<CoursePO> searchByKeyword(@Param("keyword") String keyword);
}
