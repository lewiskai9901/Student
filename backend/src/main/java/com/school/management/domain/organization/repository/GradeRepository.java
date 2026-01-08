package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.Grade;
import com.school.management.domain.organization.model.GradeStatus;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 年级仓储接口
 */
public interface GradeRepository extends Repository<Grade, Long> {

    /**
     * 根据年级编码查找
     */
    Optional<Grade> findByGradeCode(String gradeCode);

    /**
     * 根据入学年份查找
     */
    Optional<Grade> findByEnrollmentYear(Integer enrollmentYear);

    /**
     * 根据状态查找年级列表
     */
    List<Grade> findByStatus(GradeStatus status);

    /**
     * 查找所有年级
     */
    List<Grade> findAll();

    /**
     * 查找在读年级
     */
    List<Grade> findActiveGrades();

    /**
     * 根据年级主任ID查找其管理的年级
     */
    List<Grade> findByDirectorId(Long directorId);

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
    int countByStatus(GradeStatus status);

    /**
     * 查找即将毕业的年级
     */
    List<Grade> findGraduatingGrades(Integer graduationYear);

    /**
     * 按入学年份倒序查找（最新的年级在前）
     */
    List<Grade> findAllOrderByEnrollmentYearDesc();
}
