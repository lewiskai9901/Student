package com.school.management.infrastructure.persistence.student;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for Cohort persistence (DDD infrastructure layer).
 * Maps to the existing 'grades' table.
 */
@Mapper
public interface CohortPersistenceMapper extends BaseMapper<CohortPO> {

    /**
     * Find by grade code.
     */
    @Select("SELECT * FROM grades WHERE grade_code = #{gradeCode} AND deleted = 0")
    CohortPO findByGradeCode(@Param("gradeCode") String gradeCode);

    /**
     * Find by enrollment year.
     */
    @Select("SELECT * FROM grades WHERE enrollment_year = #{enrollmentYear} AND deleted = 0")
    CohortPO findByEnrollmentYear(@Param("enrollmentYear") Integer enrollmentYear);

    /**
     * Find by status.
     */
    @Select("SELECT * FROM grades WHERE status = #{status} AND deleted = 0 ORDER BY enrollment_year DESC")
    List<CohortPO> findByStatus(@Param("status") Integer status);

    /**
     * Find all cohorts ordered by enrollment year desc.
     */
    @Select("SELECT * FROM grades WHERE deleted = 0 ORDER BY enrollment_year DESC, sort_order")
    List<CohortPO> findAllOrderByEnrollmentYearDesc();

    /**
     * Find active cohorts (status = 1).
     */
    @Select("SELECT * FROM grades WHERE status = 1 AND deleted = 0 ORDER BY enrollment_year DESC")
    List<CohortPO> findActiveCohorts();

    /**
     * Find by director ID.
     */
    @Select("SELECT * FROM grades WHERE grade_director_id = #{directorId} AND deleted = 0 ORDER BY enrollment_year DESC")
    List<CohortPO> findByDirectorId(@Param("directorId") Long directorId);

    /**
     * Find graduating cohorts by graduation year.
     */
    @Select("SELECT * FROM grades WHERE graduation_year = #{graduationYear} AND status = 1 AND deleted = 0")
    List<CohortPO> findGraduatingCohorts(@Param("graduationYear") Integer graduationYear);

    /**
     * Count by status.
     */
    @Select("SELECT COUNT(*) FROM grades WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") Integer status);

    /**
     * Check if grade code exists.
     */
    @Select("SELECT COUNT(*) > 0 FROM grades WHERE grade_code = #{gradeCode} AND deleted = 0")
    boolean existsByGradeCode(@Param("gradeCode") String gradeCode);

    /**
     * Check if enrollment year exists.
     */
    @Select("SELECT COUNT(*) > 0 FROM grades WHERE enrollment_year = #{enrollmentYear} AND deleted = 0")
    boolean existsByEnrollmentYear(@Param("enrollmentYear") Integer enrollmentYear);
}
