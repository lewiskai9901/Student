package com.school.management.service.evaluation;

import com.school.management.entity.evaluation.StudentEvaluationResult;

import java.util.List;
import java.util.Map;

/**
 * 综测计算引擎服务接口
 * 负责计算学生六维度综测分数
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface EvaluationCalculationService {

    /**
     * 计算单个学生的综测结果
     *
     * @param periodId  综测周期ID
     * @param studentId 学生ID
     * @return 计算结果
     */
    StudentEvaluationResult calculateStudent(Long periodId, Long studentId);

    /**
     * 计算班级所有学生的综测结果
     *
     * @param periodId 综测周期ID
     * @param classId  班级ID
     * @return 计算结果列表
     */
    List<StudentEvaluationResult> calculateClass(Long periodId, Long classId);

    /**
     * 计算年级所有学生的综测结果
     *
     * @param periodId 综测周期ID
     * @param gradeId  年级ID
     * @return 计算结果数量
     */
    int calculateGrade(Long periodId, Long gradeId);

    /**
     * 重新计算学生综测结果
     *
     * @param resultId 结果ID
     * @return 重新计算后的结果
     */
    StudentEvaluationResult recalculate(Long resultId);

    /**
     * 计算德育分数
     *
     * @param periodId  综测周期ID
     * @param studentId 学生ID
     * @return 德育分数明细 {baseScore, bonusScore, deductScore, totalScore, weightedScore, details}
     */
    Map<String, Object> calculateMoralScore(Long periodId, Long studentId);

    /**
     * 计算智育分数
     *
     * @param periodId  综测周期ID
     * @param studentId 学生ID
     * @return 智育分数明细
     */
    Map<String, Object> calculateIntellectualScore(Long periodId, Long studentId);

    /**
     * 计算体育分数
     *
     * @param periodId  综测周期ID
     * @param studentId 学生ID
     * @return 体育分数明细
     */
    Map<String, Object> calculatePhysicalScore(Long periodId, Long studentId);

    /**
     * 计算美育分数
     *
     * @param periodId  综测周期ID
     * @param studentId 学生ID
     * @return 美育分数明细
     */
    Map<String, Object> calculateAestheticScore(Long periodId, Long studentId);

    /**
     * 计算劳育分数
     *
     * @param periodId  综测周期ID
     * @param studentId 学生ID
     * @return 劳育分数明细
     */
    Map<String, Object> calculateLaborScore(Long periodId, Long studentId);

    /**
     * 计算发展素质分数
     *
     * @param periodId  综测周期ID
     * @param studentId 学生ID
     * @return 发展素质分数明细
     */
    Map<String, Object> calculateDevelopmentScore(Long periodId, Long studentId);

    /**
     * 计算排名
     *
     * @param periodId 综测周期ID
     */
    void calculateRankings(Long periodId);

    /**
     * 获取学生的处分影响
     *
     * @param studentId 学生ID
     * @param periodId  综测周期ID
     * @return 处分影响 {hasPunishment, punishmentType, moralScoreCap, effect}
     */
    Map<String, Object> getPunishmentEffect(Long studentId, Long periodId);

    /**
     * 同步量化数据到综测
     *
     * @param periodId 综测周期ID
     * @return 同步数量
     */
    int syncQuantificationData(Long periodId);

    /**
     * 同步荣誉数据到综测
     *
     * @param periodId 综测周期ID
     * @return 同步数量
     */
    int syncHonorData(Long periodId);
}
