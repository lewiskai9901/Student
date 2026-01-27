package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.StudentGrade;
import com.school.management.domain.teaching.model.entity.GradeItem;

import java.util.List;
import java.util.Optional;

/**
 * 学生成绩仓储接口
 */
public interface StudentGradeRepository {

    /**
     * 保存成绩
     */
    StudentGrade save(StudentGrade grade);

    /**
     * 批量保存成绩
     */
    List<StudentGrade> saveAll(List<StudentGrade> grades);

    /**
     * 根据ID查询
     */
    Optional<StudentGrade> findById(Long id);

    /**
     * 根据任务和学生查询
     */
    Optional<StudentGrade> findByTaskIdAndStudentId(Long taskId, Long studentId);

    /**
     * 根据ID查询（包含明细）
     */
    Optional<StudentGrade> findByIdWithItems(Long id);

    /**
     * 查询学期的所有成绩
     */
    List<StudentGrade> findBySemesterId(Long semesterId);

    /**
     * 查询学生的所有成绩
     */
    List<StudentGrade> findByStudentId(Long studentId);

    /**
     * 查询学生某学期的成绩
     */
    List<StudentGrade> findByStudentIdAndSemesterId(Long studentId, Long semesterId);

    /**
     * 查询课程的所有成绩
     */
    List<StudentGrade> findByCourseId(Long courseId);

    /**
     * 查询任务的所有成绩
     */
    List<StudentGrade> findByTaskId(Long taskId);

    /**
     * 查询班级的成绩
     */
    List<StudentGrade> findByClassId(Long classId, Long semesterId);

    /**
     * 分页查询
     */
    List<StudentGrade> findPage(int page, int size, Long semesterId, Long classId, Long courseId, Integer status);

    /**
     * 统计总数
     */
    long count(Long semesterId, Long classId, Long courseId, Integer status);

    /**
     * 删除成绩
     */
    void deleteById(Long id);

    /**
     * 保存成绩明细
     */
    void saveGradeItems(Long gradeId, List<GradeItem> items);

    /**
     * 查询成绩明细
     */
    List<GradeItem> findGradeItemsByGradeId(Long gradeId);

    /**
     * 统计课程成绩（平均分、最高分、最低分、及格率等）
     */
    GradeStatistics calculateStatistics(Long taskId);

    /**
     * 成绩统计数据
     */
    record GradeStatistics(
            int totalCount,
            int passedCount,
            double passRate,
            double averageScore,
            double maxScore,
            double minScore
    ) {}
}
