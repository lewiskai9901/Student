package com.school.management.domain.teaching.service;

import com.school.management.domain.teaching.model.aggregate.StudentGrade;
import com.school.management.domain.teaching.model.entity.GradeItem;
import com.school.management.domain.teaching.model.valueobject.GradeScale;
import com.school.management.domain.teaching.model.valueobject.GradeWeight;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩计算领域服务
 */
public interface GradeCalculationService {

    /**
     * 计算总评成绩
     *
     * @param grade  学生成绩
     * @param weight 权重配置
     */
    void calculateTotalScore(StudentGrade grade, GradeWeight weight);

    /**
     * 根据成绩项计算总评
     *
     * @param grade 学生成绩
     * @param items 成绩项列表
     */
    void calculateFromItems(StudentGrade grade, List<GradeItem> items);

    /**
     * 转换成绩等级
     *
     * @param score     分数
     * @param scaleType 评分制类型
     * @return 等级信息
     */
    GradeScale convertToGradeScale(BigDecimal score, Integer scaleType);

    /**
     * 计算绩点
     *
     * @param score     分数
     * @param scaleType 评分制类型
     * @return 绩点
     */
    BigDecimal calculateGradePoint(BigDecimal score, Integer scaleType);

    /**
     * 计算平均绩点（GPA）
     *
     * @param grades 成绩列表
     * @return GPA
     */
    BigDecimal calculateGPA(List<StudentGrade> grades);

    /**
     * 计算加权平均分
     *
     * @param grades 成绩列表
     * @return 加权平均分
     */
    BigDecimal calculateWeightedAverage(List<StudentGrade> grades);

    /**
     * 检查是否通过
     *
     * @param score     分数
     * @param passLine  及格线
     * @return 是否通过
     */
    boolean checkPassed(BigDecimal score, BigDecimal passLine);

    /**
     * 批量更新成绩等级和绩点
     *
     * @param grades 成绩列表
     */
    void updateGradeScales(List<StudentGrade> grades);

    /**
     * 计算班级成绩统计
     *
     * @param grades 班级成绩列表
     * @return 统计结果
     */
    ClassGradeStatistics calculateClassStatistics(List<StudentGrade> grades);

    /**
     * 班级成绩统计
     */
    record ClassGradeStatistics(
            int totalCount,
            int passedCount,
            double passRate,
            double averageScore,
            double maxScore,
            double minScore,
            double standardDeviation,
            int excellentCount,  // 优秀人数（90+）
            int goodCount,       // 良好人数（80-89）
            int mediumCount,     // 中等人数（70-79）
            int passCount,       // 及格人数（60-69）
            int failCount        // 不及格人数（<60）
    ) {}
}
