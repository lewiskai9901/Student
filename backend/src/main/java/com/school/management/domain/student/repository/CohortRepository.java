package com.school.management.domain.student.repository;

import com.school.management.domain.student.model.Cohort;
import com.school.management.domain.student.model.CohortStatus;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 年级仓储接口
 */
public interface CohortRepository extends Repository<Cohort, Long> {

    /**
     * 根据年级编码查找
     */
    Optional<Cohort> findByGradeCode(String gradeCode);

    /**
     * 根据入学年份查找
     */
    Optional<Cohort> findByEnrollmentYear(Integer enrollmentYear);

    /**
     * 根据状态查找年级列表
     */
    List<Cohort> findByStatus(CohortStatus status);

    /**
     * 查找所有年级
     */
    List<Cohort> findAll();

    /**
     * 查找在读年级
     */
    List<Cohort> findActiveCohorts();

    /**
     * 根据年级主任ID查找其管理的年级
     */
    List<Cohort> findByDirectorId(Long directorId);

    /**
     * 检查年级编码是否存在
     */
    boolean existsByGradeCode(String gradeCode);

    /**
     * 检查入学年份是否已存在
     */
    boolean existsByEnrollmentYear(Integer enrollmentYear);

    /**
     * 统计某状态的年级数量
     */
    int countByStatus(CohortStatus status);

    /**
     * 查找即将毕业的年级
     */
    List<Cohort> findGraduatingCohorts(Integer graduationYear);

    /**
     * 按入学年份倒序查找（最新的年级在前）
     */
    List<Cohort> findAllOrderByEnrollmentYearDesc();
}
