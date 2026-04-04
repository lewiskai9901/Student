package com.school.management.domain.student.repository;

import com.school.management.domain.student.model.ClassStatus;
import com.school.management.domain.student.model.SchoolClass;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 班级仓储接口
 */
public interface SchoolClassRepository extends Repository<SchoolClass, Long> {

    /**
     * 根据班级编码查找
     */
    Optional<SchoolClass> findByClassCode(String classCode);

    /**
     * 根据组织单元ID查找班级列表
     */
    List<SchoolClass> findByOrgUnitId(Long orgUnitId);

    /**
     * 根据年级ID查找班级列表
     */
    List<SchoolClass> findByGradeId(Long gradeId);

    /**
     * 根据入学年份查找班级列表
     */
    List<SchoolClass> findByEnrollmentYear(Integer enrollmentYear);

    /**
     * 根据状态查找班级列表
     */
    List<SchoolClass> findByStatus(ClassStatus status);

    /**
     * 统计年级下的班级数量
     */
    int countByGradeId(Long gradeId);

    /**
     * 根据组织单元和入学年份查找
     */
    List<SchoolClass> findByOrgUnitIdAndEnrollmentYear(Long orgUnitId, Integer enrollmentYear);

    /**
     * 根据班主任ID查找其管理的班级
     */
    List<SchoolClass> findByHeadTeacherId(Long teacherId);

    /**
     * 根据教师ID查找其管理的班级（班主任或副班主任）
     */
    List<SchoolClass> findByTeacherId(Long teacherId);

    /**
     * 根据专业方向ID查找班级
     */
    List<SchoolClass> findByMajorDirectionId(Long majorDirectionId);

    /**
     * 检查班级编码是否存在
     */
    boolean existsByClassCode(String classCode);

    /**
     * 统计组织单元下的班级数量
     */
    int countByOrgUnitId(Long orgUnitId);

    /**
     * 统计某状态的班级数量
     */
    int countByStatus(ClassStatus status);

    /**
     * 查找即将毕业的班级（根据入学年份和学制）
     */
    List<SchoolClass> findGraduatingClasses(Integer graduationYear);
}
