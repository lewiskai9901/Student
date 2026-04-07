package com.school.management.infrastructure.persistence.teaching.grade;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
@DataPermission(module = "student_grade", orgUnitField = "org_unit_id")
public interface StudentGradeMapper extends BaseMapper<StudentGradePO> {

    @Select("SELECT g.id, g.batch_id AS batchId, g.semester_id AS semesterId, " +
            "g.task_id AS taskId, g.course_id AS courseId, g.student_id AS studentId, " +
            "g.org_unit_id AS orgUnitId, g.total_score AS totalScore, " +
            "g.grade_point AS gradePoint, g.passed, g.credits_earned AS creditsEarned, " +
            "g.grade_status AS gradeStatus, g.remark, " +
            "g.created_at AS createdAt, g.updated_at AS updatedAt, " +
            "s.name AS studentName, s.student_no AS studentNo " +
            "FROM student_grades g " +
            "LEFT JOIN students s ON g.student_id = s.id " +
            "WHERE g.batch_id = #{batchId} AND g.deleted = 0 ORDER BY g.created_at DESC")
    List<Map<String, Object>> listByBatchWithStudentInfo(@Param("batchId") Long batchId);

    @Select("SELECT g.id, g.batch_id AS batchId, g.semester_id AS semesterId, " +
            "g.course_id AS courseId, g.student_id AS studentId, " +
            "g.org_unit_id AS orgUnitId, g.total_score AS totalScore, " +
            "g.grade_point AS gradePoint, g.passed, g.credits_earned AS creditsEarned, " +
            "g.grade_status AS gradeStatus, g.remark, " +
            "c.course_name AS courseName, " +
            "s.name AS studentName, s.student_no AS studentNo " +
            "FROM student_grades g " +
            "LEFT JOIN courses c ON g.course_id = c.id " +
            "LEFT JOIN students s ON g.student_id = s.id " +
            "WHERE g.org_unit_id = #{classId} AND g.deleted = 0 " +
            "ORDER BY g.course_id, g.total_score DESC")
    List<Map<String, Object>> listByClassWithJoins(@Param("orgUnitId") Long orgUnitId);

    @Select("SELECT sg.total_score, sg.grade_point, sg.passed, " +
            "s.student_no, s.name AS student_name, " +
            "c.course_name, sc.name AS class_name " +
            "FROM student_grades sg " +
            "LEFT JOIN students s ON s.id = sg.student_id " +
            "LEFT JOIN courses c ON c.id = sg.course_id " +
            "LEFT JOIN school_classes sc ON sc.id = sg.org_unit_id " +
            "WHERE sg.semester_id = #{semesterId} AND sg.deleted = 0 " +
            "ORDER BY sc.name, s.student_no")
    List<Map<String, Object>> listForExport(@Param("semesterId") Long semesterId);
}
