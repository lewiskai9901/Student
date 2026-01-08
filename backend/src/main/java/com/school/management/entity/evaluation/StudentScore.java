package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生成绩实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("student_scores")
public class StudentScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 课程安排ID
     */
    private Long arrangementId;

    /**
     * 平时成绩
     */
    private BigDecimal regularScore;

    /**
     * 平时成绩权重
     */
    private BigDecimal regularWeight;

    /**
     * 期中成绩
     */
    private BigDecimal midtermScore;

    /**
     * 期中成绩权重
     */
    private BigDecimal midtermWeight;

    /**
     * 期末成绩
     */
    private BigDecimal finalScore;

    /**
     * 期末成绩权重
     */
    private BigDecimal finalWeight;

    /**
     * 总评成绩
     */
    private BigDecimal totalScore;

    /**
     * 补考成绩
     */
    private BigDecimal makeupScore;

    /**
     * 重修成绩
     */
    private BigDecimal retakeScore;

    /**
     * 最终成绩(含补考重修)
     */
    private BigDecimal finalTotalScore;

    /**
     * 绩点
     */
    private BigDecimal gradePoint;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 学分绩点
     */
    private BigDecimal creditPoint;

    /**
     * 成绩状态: 0未录入,1已录入,2已确认,3缓考,4免修,5缺考
     */
    private Integer scoreStatus;

    /**
     * 是否及格
     */
    private Integer isPassed;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 录入人
     */
    private Long inputBy;

    /**
     * 录入时间
     */
    private LocalDateTime inputAt;

    /**
     * 确认人
     */
    private Long confirmedBy;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 成绩状态常量 ====================

    public static final int STATUS_NOT_INPUT = 0;     // 未录入
    public static final int STATUS_INPUT = 1;         // 已录入
    public static final int STATUS_CONFIRMED = 2;     // 已确认
    public static final int STATUS_DEFERRED = 3;      // 缓考
    public static final int STATUS_EXEMPTED = 4;      // 免修
    public static final int STATUS_ABSENT = 5;        // 缺考

    /**
     * 计算总评成绩
     */
    public void calculateTotalScore() {
        if (regularScore != null && midtermScore != null && finalScore != null) {
            BigDecimal rWeight = regularWeight != null ? regularWeight : new BigDecimal("0.30");
            BigDecimal mWeight = midtermWeight != null ? midtermWeight : new BigDecimal("0.20");
            BigDecimal fWeight = finalWeight != null ? finalWeight : new BigDecimal("0.50");

            this.totalScore = regularScore.multiply(rWeight)
                    .add(midtermScore.multiply(mWeight))
                    .add(finalScore.multiply(fWeight))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            this.isPassed = this.totalScore.compareTo(new BigDecimal("60")) >= 0 ? 1 : 0;
        }
    }

    /**
     * 计算绩点
     */
    public void calculateGradePoint() {
        if (totalScore == null) return;

        double score = totalScore.doubleValue();
        if (score >= 90) {
            this.gradePoint = new BigDecimal("4.0");
        } else if (score >= 85) {
            this.gradePoint = new BigDecimal("3.7");
        } else if (score >= 82) {
            this.gradePoint = new BigDecimal("3.3");
        } else if (score >= 78) {
            this.gradePoint = new BigDecimal("3.0");
        } else if (score >= 75) {
            this.gradePoint = new BigDecimal("2.7");
        } else if (score >= 72) {
            this.gradePoint = new BigDecimal("2.3");
        } else if (score >= 68) {
            this.gradePoint = new BigDecimal("2.0");
        } else if (score >= 64) {
            this.gradePoint = new BigDecimal("1.5");
        } else if (score >= 60) {
            this.gradePoint = new BigDecimal("1.0");
        } else {
            this.gradePoint = new BigDecimal("0.0");
        }

        // 计算学分绩点
        if (credit != null) {
            this.creditPoint = this.gradePoint.multiply(credit).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }
}
