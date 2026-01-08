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
     * Find by department ID.
     */
    @Select("SELECT * FROM classes WHERE department_id = #{departmentId} AND deleted = 0 ORDER BY class_code")
    List<SchoolClassPO> findByDepartmentId(@Param("departmentId") Long departmentId);

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
     * Count by department ID.
     */
    @Select("SELECT COUNT(*) FROM classes WHERE department_id = #{departmentId} AND deleted = 0")
    int countByDepartmentId(@Param("departmentId") Long departmentId);

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
}
