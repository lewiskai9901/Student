package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.Course;

import java.util.List;
import java.util.Optional;

/**
 * 课程仓储接口
 */
public interface CourseRepository {

    /**
     * 保存课程
     */
    Course save(Course course);

    /**
     * 根据ID查询
     */
    Optional<Course> findById(Long id);

    /**
     * 根据课程代码查询
     */
    Optional<Course> findByCourseCode(String courseCode);

    /**
     * 查询所有课程
     */
    List<Course> findAll();

    /**
     * 根据课程类别查询
     */
    List<Course> findByCategory(Integer category);

    /**
     * 根据开课部门查询
     */
    List<Course> findByOrgUnitId(Long orgUnitId);

    /**
     * 根据课程性质查询
     */
    List<Course> findByType(Integer courseType);

    /**
     * 分页查询
     */
    List<Course> findPage(int page, int size, String keyword, Integer category, Integer status);

    /**
     * 统计总数
     */
    long count(String keyword, Integer category, Integer status);

    /**
     * 删除课程
     */
    void deleteById(Long id);

    /**
     * 检查课程代码是否存在
     */
    boolean existsByCourseCode(String courseCode);

    /**
     * 根据ID列表查询
     */
    List<Course> findByIds(List<Long> ids);
}
