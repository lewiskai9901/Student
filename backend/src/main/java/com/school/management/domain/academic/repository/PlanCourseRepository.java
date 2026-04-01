package com.school.management.domain.academic.repository;

import com.school.management.domain.academic.model.PlanCourse;

import java.util.List;
import java.util.Optional;

/**
 * 方案课程仓储接口
 */
public interface PlanCourseRepository {

    Optional<PlanCourse> findById(Long id);

    PlanCourse save(PlanCourse planCourse);

    void deleteById(Long id);

    /**
     * 查找方案的所有课程（含 JOIN 课程名称）
     */
    List<PlanCourse> findByPlanId(Long planId);

    /**
     * 删除方案的所有课程
     */
    void deleteByPlanId(Long planId);
}
