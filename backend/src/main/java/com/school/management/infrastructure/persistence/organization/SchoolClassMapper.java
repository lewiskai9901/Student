package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for SchoolClass persistence.
 * Maps to the existing 'classes' table.
 */
@Mapper
public interface SchoolClassMapper extends BaseMapper<SchoolClassPO> {

    /**
     * Find all enabled classes.
     */
    @Select("SELECT * FROM classes WHERE status = 1 AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findAllEnabled();

    /**
     * Find by class code.
     */
    @Select("SELECT * FROM classes WHERE class_code = #{code} AND deleted = 0")
    SchoolClassPO findByClassCode(@Param("code") String code);

    /**
     * Find by org unit ID.
     */
    @Select("SELECT * FROM classes WHERE org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * Find by teacher ID (head teacher).
     */
    @Select("SELECT * FROM classes WHERE teacher_id = #{teacherId} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Find by enrollment year.
     */
    @Select("SELECT * FROM classes WHERE enrollment_year = #{year} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByEnrollmentYear(@Param("year") Integer year);

    /**
     * Count by org unit ID.
     */
    @Select("SELECT COUNT(*) FROM classes WHERE org_unit_id = #{orgUnitId} AND deleted = 0")
    int countByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * Count by status.
     */
    @Select("SELECT COUNT(*) FROM classes WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") Integer status);

    /**
     * Find by grade ID.
     */
    @Select("SELECT * FROM classes WHERE grade_id = #{gradeId} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByGradeId(@Param("gradeId") Long gradeId);

    /**
     * Count by grade ID.
     */
    @Select("SELECT COUNT(*) FROM classes WHERE grade_id = #{gradeId} AND deleted = 0")
    int countByGradeId(@Param("gradeId") Long gradeId);

    /**
     * Find by status.
     */
    @Select("SELECT * FROM classes WHERE status = #{status} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByStatus(@Param("status") Integer status);

    /**
     * Find by major direction ID.
     */
    @Select("SELECT * FROM classes WHERE major_direction_id = #{majorDirectionId} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByMajorDirectionId(@Param("majorDirectionId") Long majorDirectionId);

    /**
     * Find by graduation year.
     */
    @Select("SELECT * FROM classes WHERE graduation_year = #{graduationYear} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByGraduationYear(@Param("graduationYear") Integer graduationYear);
}
