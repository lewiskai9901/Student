package com.school.management.domain.semester.repository;

import com.school.management.domain.semester.model.aggregate.Semester;
import com.school.management.domain.shared.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 学期仓储接口
 */
public interface SemesterRepository extends Repository<Semester, Long> {

    /**
     * 保存学期
     */
    Semester save(Semester semester);

    /**
     * 根据ID查找学期
     */
    Optional<Semester> findById(Long id);

    /**
     * 根据学期编码查找学期
     */
    Optional<Semester> findBySemesterCode(String semesterCode);

    /**
     * 获取当前学期
     */
    Optional<Semester> findCurrentSemester();

    /**
     * 检查学期编码是否存在
     */
    boolean existsBySemesterCode(String semesterCode);

    /**
     * 检查学期编码是否存在（排除指定ID）
     */
    boolean existsBySemesterCodeAndIdNot(String semesterCode, Long excludeId);

    /**
     * 根据日期范围查找学期
     */
    List<Semester> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 根据年份查找学期
     */
    List<Semester> findByStartYear(Integer startYear);

    /**
     * 获取所有正常状态的学期
     */
    List<Semester> findAllActive();

    /**
     * 获取所有学期（按开始日期降序）
     */
    List<Semester> findAllOrderByStartDateDesc();

    /**
     * 删除学期
     */
    void deleteById(Long id);

    /**
     * 分页查询学期
     */
    List<Semester> findAll(int page, int size);

    /**
     * 查询学期总数
     */
    long count();

    /**
     * 取消所有当前学期标识（用于设置新的当前学期前）
     */
    void clearAllCurrentFlags();
}
