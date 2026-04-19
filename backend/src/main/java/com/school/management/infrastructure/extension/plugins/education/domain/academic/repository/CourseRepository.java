package com.school.management.domain.academic.repository;

import com.school.management.domain.academic.model.Course;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课程仓储接口
 */
public interface CourseRepository extends Repository<Course, Long> {

    /**
     * 根据课程代码查找
     */
    Optional<Course> findByCourseCode(String courseCode);

    /**
     * 检查课程代码是否已存在
     */
    boolean existsByCourseCode(String courseCode);

    /**
     * 查找所有启用的课程
     */
    List<Course> findAllEnabled();
}
