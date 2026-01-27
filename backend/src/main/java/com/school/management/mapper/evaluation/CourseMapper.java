package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 课程Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 分页查询课程
     */
    Page<Map<String, Object>> selectCoursePage(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 根据课程编码查询
     */
    Course selectByCode(@Param("code") String code);

    /**
     * 查询组织单元的课程列表
     */
    List<Course> selectByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据类型查询课程
     */
    List<Course> selectByType(@Param("type") Integer type);

    /**
     * 查询课程详情
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 根据学期查询课程
     */
    List<Course> selectBySemesterId(@Param("semesterId") Long semesterId);

    /**
     * 根据班级查询课程
     */
    List<Map<String, Object>> selectByClassId(@Param("classId") Long classId, @Param("semesterId") Long semesterId);

    /**
     * 统计课程的成绩记录数
     */
    int countScoresByCourseId(@Param("courseId") Long courseId);
}
