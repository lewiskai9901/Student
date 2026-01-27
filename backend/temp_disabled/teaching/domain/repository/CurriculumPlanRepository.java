package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.CurriculumPlan;

import java.util.List;
import java.util.Optional;

/**
 * 培养方案仓储接口
 */
public interface CurriculumPlanRepository {

    /**
     * 保存培养方案
     */
    CurriculumPlan save(CurriculumPlan plan);

    /**
     * 根据ID查询
     */
    Optional<CurriculumPlan> findById(Long id);

    /**
     * 根据方案代码查询
     */
    Optional<CurriculumPlan> findByPlanCode(String planCode);

    /**
     * 根据ID查询（包含课程）
     */
    Optional<CurriculumPlan> findByIdWithCourses(Long id);

    /**
     * 查询所有培养方案
     */
    List<CurriculumPlan> findAll();

    /**
     * 根据专业查询
     */
    List<CurriculumPlan> findByMajorId(Long majorId);

    /**
     * 根据专业和年级查询
     */
    Optional<CurriculumPlan> findByMajorIdAndGradeYear(Long majorId, Integer gradeYear);

    /**
     * 根据专业和入学年份查询（别名方法）
     */
    default Optional<CurriculumPlan> findByMajorIdAndEnrollYear(Long majorId, Integer enrollYear) {
        return findByMajorIdAndGradeYear(majorId, enrollYear);
    }

    /**
     * 分页查询
     */
    List<CurriculumPlan> findPage(int page, int size, Long majorId, Integer status);

    /**
     * 分页查询（带入学年份过滤）
     */
    List<CurriculumPlan> findPage(int page, int size, Long majorId, Integer enrollYear, Integer status);

    /**
     * 统计总数
     */
    long count(Long majorId, Integer status);

    /**
     * 统计总数（带入学年份过滤）
     */
    long count(Long majorId, Integer enrollYear, Integer status);

    /**
     * 获取方案课程列表
     */
    List<com.school.management.domain.teaching.model.entity.PlanCourse> findPlanCourses(Long planId);

    /**
     * 删除培养方案
     */
    void deleteById(Long id);

    /**
     * 检查方案代码是否存在
     */
    boolean existsByPlanCode(String planCode);

    /**
     * 保存方案课程
     */
    void savePlanCourses(Long planId, List<com.school.management.domain.teaching.model.entity.PlanCourse> courses);

    /**
     * 删除方案课程
     */
    void deletePlanCourse(Long planId, Long courseId);
}
