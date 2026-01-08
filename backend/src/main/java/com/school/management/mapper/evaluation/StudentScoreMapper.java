package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.StudentScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 学生成绩Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface StudentScoreMapper extends BaseMapper<StudentScore> {

    /**
     * 分页查询成绩
     */
    Page<Map<String, Object>> selectScorePage(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 查询学生某学期的所有成绩
     */
    List<Map<String, Object>> selectByStudentAndSemester(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /**
     * 查询班级某课程的成绩列表
     */
    List<Map<String, Object>> selectByClassAndCourse(@Param("classId") Long classId, @Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

    /**
     * 批量插入成绩
     */
    void batchInsert(@Param("scores") List<StudentScore> scores);

    /**
     * 批量更新成绩
     */
    void batchUpdate(@Param("scores") List<StudentScore> scores);

    /**
     * 计算学生学期成绩汇总
     */
    Map<String, Object> calculateSemesterSummary(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /**
     * 统计班级成绩
     */
    Map<String, Object> selectClassScoreStatistics(@Param("classId") Long classId, @Param("semesterId") Long semesterId);

    /**
     * 查询成绩详情
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 根据学生和课程查询成绩
     */
    StudentScore selectByStudentAndCourse(@Param("studentId") Long studentId,
                                           @Param("courseId") Long courseId,
                                           @Param("semesterId") Long semesterId);

    /**
     * 统计班级课程成绩
     */
    Map<String, Object> selectClassStatistics(@Param("classId") Long classId,
                                               @Param("courseId") Long courseId,
                                               @Param("semesterId") Long semesterId);

    /**
     * 查询课程成绩排名
     */
    List<Map<String, Object>> selectCourseRanking(@Param("courseId") Long courseId,
                                                   @Param("semesterId") Long semesterId,
                                                   @Param("limit") Integer limit);

    /**
     * 导出班级成绩
     */
    List<Map<String, Object>> selectForExport(@Param("classId") Long classId,
                                               @Param("semesterId") Long semesterId);

    /**
     * 统计学生学期成绩数量
     */
    int countByStudentAndSemester(@Param("studentId") Long studentId,
                                   @Param("semesterId") Long semesterId);

    /**
     * 锁定成绩
     */
    void lockBySemesterAndCourse(@Param("semesterId") Long semesterId,
                                  @Param("courseId") Long courseId);

    /**
     * 解锁成绩
     */
    void unlockBySemesterAndCourse(@Param("semesterId") Long semesterId,
                                    @Param("courseId") Long courseId);
}
