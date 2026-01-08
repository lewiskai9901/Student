package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.Course;

import java.util.List;
import java.util.Map;

/**
 * 课程服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface CourseService extends IService<Course> {

    /**
     * 分页查询课程
     */
    Page<Map<String, Object>> pageCourses(Page<?> page, Map<String, Object> query);

    /**
     * 创建课程
     */
    Long createCourse(Course course);

    /**
     * 更新课程
     */
    void updateCourse(Course course);

    /**
     * 删除课程
     */
    void deleteCourse(Long id);

    /**
     * 获取课程详情
     */
    Map<String, Object> getCourseDetail(Long id);

    /**
     * 根据学期查询课程列表
     */
    List<Course> getBySemesterId(Long semesterId);

    /**
     * 根据班级查询开设课程
     */
    List<Map<String, Object>> getByClassId(Long classId, Long semesterId);

    /**
     * 检查课程编码是否存在
     */
    boolean existsByCode(String courseCode, Long excludeId);

    /**
     * 导入课程数据
     */
    int importCourses(List<Course> courses, Long semesterId);

    /**
     * 获取课程类型列表
     */
    List<Map<String, Object>> getCourseTypes();
}
