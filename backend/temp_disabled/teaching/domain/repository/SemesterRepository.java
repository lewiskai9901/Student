package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.Semester;

import java.util.List;
import java.util.Optional;

/**
 * 学期仓储接口
 */
public interface SemesterRepository {

    /**
     * 保存学期
     */
    Semester save(Semester semester);

    /**
     * 根据ID查询
     */
    Optional<Semester> findById(Long id);

    /**
     * 根据学期代码查询
     */
    Optional<Semester> findBySemesterCode(String semesterCode);

    /**
     * 查询学年下的所有学期
     */
    List<Semester> findByAcademicYearId(Long academicYearId);

    /**
     * 查询所有学期
     */
    List<Semester> findAll();

    /**
     * 查询当前学期
     */
    Optional<Semester> findCurrent();

    /**
     * 删除学期
     */
    void deleteById(Long id);

    /**
     * 清除所有当前学期标记
     */
    void clearAllCurrent();

    /**
     * 检查学期代码是否存在
     */
    boolean existsBySemesterCode(String semesterCode);

    /**
     * 查询学期（包含教学周信息）
     */
    Optional<Semester> findByIdWithWeeks(Long id);

    /**
     * 保存教学周
     */
    void saveTeachingWeeks(Long semesterId, List<com.school.management.domain.teaching.model.entity.TeachingWeek> weeks);
}
