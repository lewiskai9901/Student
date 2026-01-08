package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.StudentScore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 学生成绩服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface StudentScoreService extends IService<StudentScore> {

    /**
     * 分页查询成绩
     */
    Page<Map<String, Object>> pageScores(Page<?> page, Map<String, Object> query);

    /**
     * 录入成绩
     */
    Long inputScore(StudentScore score);

    /**
     * 批量录入成绩
     */
    int batchInputScores(List<StudentScore> scores);

    /**
     * 更新成绩
     */
    void updateScore(StudentScore score);

    /**
     * 删除成绩
     */
    void deleteScore(Long id);

    /**
     * 获取成绩详情
     */
    Map<String, Object> getScoreDetail(Long id);

    /**
     * 获取学生在某学期的所有成绩
     */
    List<Map<String, Object>> getStudentScores(Long studentId, Long semesterId);

    /**
     * 获取学生在某周期的所有成绩
     */
    List<Map<String, Object>> getStudentScoresByPeriod(Long studentId, Long periodId);

    /**
     * 计算学生学期GPA
     */
    BigDecimal calculateSemesterGPA(Long studentId, Long semesterId);

    /**
     * 计算学生学期平均分
     */
    BigDecimal calculateSemesterAverage(Long studentId, Long semesterId);

    /**
     * 计算学生学期加权平均分
     */
    BigDecimal calculateWeightedAverage(Long studentId, Long semesterId);

    /**
     * 获取班级成绩统计
     */
    Map<String, Object> getClassScoreStatistics(Long classId, Long courseId, Long semesterId);

    /**
     * 获取课程成绩排名
     */
    List<Map<String, Object>> getCourseScoreRanking(Long courseId, Long semesterId, Integer limit);

    /**
     * 导入成绩数据
     */
    int importScores(List<StudentScore> scores);

    /**
     * 导出班级成绩
     */
    List<Map<String, Object>> exportClassScores(Long classId, Long semesterId);

    /**
     * 检查成绩是否已录入
     */
    boolean existsScore(Long studentId, Long courseId, Long semesterId);

    /**
     * 获取学生智育分数（用于综测计算）
     */
    Map<String, Object> calculateIntellectualScoreForEvaluation(Long studentId, Long periodId);

    /**
     * 锁定成绩（防止修改）
     */
    void lockScores(Long semesterId, Long courseId);

    /**
     * 解锁成绩
     */
    void unlockScores(Long semesterId, Long courseId);
}
