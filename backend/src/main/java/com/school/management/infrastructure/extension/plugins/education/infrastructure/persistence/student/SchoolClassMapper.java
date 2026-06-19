package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.student;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for SchoolClass persistence.
 * Maps to the existing 'classes' table.
 */
@Mapper
// classes 是 org_units 视图: id=班级自身 org (member 挂它), org_unit_id=父年级。
// 数据权限须按班级自身 org 过滤 (orgField IN 子树), 故 orgUnitField=id 不是 org_unit_id —
// 否则 CUSTOM=某班看不到该班自身, 且 DEPARTMENT 被错当成"下一级"。详见 SchoolClassDataPermissionOrgFieldTest。
@DataPermission(module = "school_class")
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
     * NOTE: enrollment_year is no longer in the classes VIEW; query via org_units.attributes instead.
     */
    @Select("SELECT c.* FROM classes c " +
            "JOIN org_units o ON c.id = o.id AND o.deleted = 0 " +
            "WHERE JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.enrollmentYear')) = #{year} " +
            "AND c.deleted = 0 ORDER BY c.class_code")
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
     * NOTE: major_direction_id is no longer in the classes VIEW; query via org_units.attributes instead.
     */
    @Select("SELECT c.* FROM classes c " +
            "JOIN org_units o ON c.id = o.id AND o.deleted = 0 " +
            "WHERE CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.majorDirectionId')) AS UNSIGNED) = #{majorDirectionId} " +
            "AND c.deleted = 0 ORDER BY c.class_code")
    List<SchoolClassPO> findByMajorDirectionId(@Param("majorDirectionId") Long majorDirectionId);

    /**
     * Find by graduation year.
     * NOTE: graduation_year is no longer in the classes VIEW; query via org_units.attributes instead.
     */
    @Select("SELECT c.* FROM classes c " +
            "JOIN org_units o ON c.id = o.id AND o.deleted = 0 " +
            "WHERE JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.graduationYear')) = #{graduationYear} " +
            "AND c.deleted = 0 ORDER BY c.class_code")
    List<SchoolClassPO> findByGraduationYear(@Param("graduationYear") Integer graduationYear);
}
